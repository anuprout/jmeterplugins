package com.arsoftwares.jmeterplugins.visualizers.backendlisteners.azuremonitor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.services.FileServer;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jmeter.visualizers.backend.AbstractBackendListenerClient;
import org.apache.jmeter.visualizers.backend.BackendListenerContext;
import org.apache.jmeter.visualizers.backend.SamplerMetric;
import org.apache.jmeter.visualizers.backend.UserMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AzureMonitorBackendListenerClient extends AbstractBackendListenerClient implements Runnable{
	
	private static final Logger log = LoggerFactory.getLogger(AzureMonitorBackendListenerClient.class);
	
	private static final Map<String, String> DEFAULT_ARGS = new LinkedHashMap<String,String>();
    static {
        DEFAULT_ARGS.put("azureMonitorUrl", "https://monitoring.azure.com/subscriptions/xxxx/resourcegroups/yyyy/providers/Microsoft.web/appServices/zzzz/metrics");
        DEFAULT_ARGS.put("azureMonitorAccessKey", "");
        DEFAULT_ARGS.put("summaryOnly", "false");
        DEFAULT_ARGS.put("captureConcurrentUsers", "true");
        DEFAULT_ARGS.put("captureResponseTime", "true");
        DEFAULT_ARGS.put("captureHitsPerSecond", "true");
        DEFAULT_ARGS.put("samplersRegEx", ".*");
        DEFAULT_ARGS.put("testTitle", "Test name");
        
    }
    
    private static final Object LOCK = new Object();
    
    private boolean summaryOnly;
    private boolean captureConcurrentUsers;
    private boolean captureResponseTime;
    private boolean captureHitsPerSecond;
    private Pattern samplersRegExFilter;
    
    private ConcurrentHashMap<String, SamplerMetric> metricsPerSampler = new ConcurrentHashMap<String, SamplerMetric>();
        
    private static final int MAX_POOL_SIZE = 1;
    private static final long METRICS_SEND_INTERVAL = JMeterUtils.getPropDefault("backend_azuremonitor.metrics_send_interval", 5);
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> timerHandle;
    
    private AzureMonitorMetricSender azureMonitorMetricSender ;
    
    
    public AzureMonitorBackendListenerClient() {
        super();
    }
	
	@Override
    public Arguments getDefaultParameters() {
        Arguments arguments = new Arguments();
        DEFAULT_ARGS.forEach(arguments::addArgument);
        return arguments;
    }
	
	@Override
	public void handleSampleResults(List<SampleResult> sampleResults, BackendListenerContext context) {
		synchronized (LOCK) {
            UserMetric userMetrics = getUserMetrics();
            for (SampleResult sampleResult : sampleResults) {
            	//log.info("Handling sampler result for "+sampleResult.getSampleLabel());
            	try {
	                userMetrics.add(sampleResult);
	                
	                //check if the sampler has been chosen to be monitored.
	                Matcher matcher = samplersRegExFilter.matcher(sampleResult.getSampleLabel());
	                if (!summaryOnly && (matcher.find())) {
	                	//log.info("adding metric for "+sampleResult.getSampleLabel());
	                	metricsPerSampler.putIfAbsent(sampleResult.getSampleLabel(), new SamplerMetric());
	                    metricsPerSampler.get(sampleResult.getSampleLabel()).add(sampleResult);
	                    
	                    
	                }
            	}catch(Exception e) {
            		log.error("Error while capturing sampler result in AzureMonitor backend listener.", e);
            	}
                
            }
        }
		
	}

	@Override
	public void run() {
		
		long timestamp = System.currentTimeMillis();
		
		//add sampler metrics to azure monitor metric cache
		synchronized (LOCK) {
            for (Map.Entry<String, SamplerMetric> entry : metricsPerSampler.entrySet()) {
                SamplerMetric metric = entry.getValue();
                String samplerName = entry.getKey();
                try {
                	
                	if(captureResponseTime) {
                		azureMonitorMetricSender.addResponseTimeMetric(timestamp, samplerName, metric);
                	}
                	
                	if(captureHitsPerSecond) {
                		azureMonitorMetricSender.addThroughputMetric(timestamp, samplerName, (double) (metric.getHits() / METRICS_SEND_INTERVAL));
                	}
                	
                	// We are computing on interval basis so cleanup
                    metric.resetForTimeInterval();
                    
                }catch(Exception e) {
                	log.error("Error while adding sampler metric in AzureMonitor backend listener.", e);
                }
                
            }
            
        }
		
		//add user metric to azure monitor metric cache
		try {
			if(captureConcurrentUsers) {
				UserMetric userMetrics = getUserMetrics();
				azureMonitorMetricSender.addUserMetric(timestamp, getUserMetrics());
				userMetrics.resetForTimeInterval();
			}
			
		} catch (Exception e) {
			log.error("Error while adding user metric in AzureMonitor backend listener.", e);
		}
		
		//send metrics to Azure Monitor
		azureMonitorMetricSender.sendMetrics();
    	
		
	}
	
	@Override
	public void setupTest(BackendListenerContext context) throws Exception {
		
		String azureMonitorUrl = context.getParameter("azureMonitorUrl");
		String azureMonitorAccessKey = context.getParameter("azureMonitorAccessKey");
		this.summaryOnly = context.getBooleanParameter("summaryOnly",false);
		this.captureConcurrentUsers = context.getBooleanParameter("captureConcurrentUsers",true);
		this.captureResponseTime = context.getBooleanParameter("captureResponseTime",true);
		this.captureHitsPerSecond = context.getBooleanParameter("captureHitsPerSecond",true);
		this.samplersRegExFilter = Pattern.compile(context.getParameter("samplersRegEx",".*"));
		
		this.azureMonitorMetricSender = new AzureMonitorMetricSender(context.getParameter("testTitle",FileServer.getFileServer().getScriptName()));
		this.azureMonitorMetricSender.setup(azureMonitorUrl,azureMonitorAccessKey);
		
		
		//create a scheduler backend listener and set metrics send interval
		scheduler = Executors.newScheduledThreadPool(MAX_POOL_SIZE);
		this.timerHandle = scheduler.scheduleAtFixedRate(this, 0, METRICS_SEND_INTERVAL, TimeUnit.SECONDS);
		
	}
	
	@Override
	public void teardownTest(BackendListenerContext context) throws Exception{
		boolean cancelState = timerHandle.cancel(false);
        log.debug("AzureMoniotr backend listener canceled state: {}", cancelState);
        scheduler.shutdown();
        try {
            scheduler.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Error waiting for end of scheduler");
            Thread.currentThread().interrupt();
        }

        
        // Send last set of data before ending
        log.info("Sending last metrics");
        azureMonitorMetricSender.sendMetrics();

        azureMonitorMetricSender.destroy();
        super.teardownTest(context);
		
	}

}
