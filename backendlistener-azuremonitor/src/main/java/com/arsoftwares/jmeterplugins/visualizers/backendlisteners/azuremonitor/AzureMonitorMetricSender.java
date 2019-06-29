package com.arsoftwares.jmeterplugins.visualizers.backendlisteners.azuremonitor;

import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jmeter.visualizers.backend.SamplerMetric;
import org.apache.jmeter.visualizers.backend.UserMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.arsoftwares.jmeterplugins.visualizers.backendlisteners.azuremonitor.metricmodels.JmeterMetric;
import com.arsoftwares.jmeterplugins.visualizers.backendlisteners.azuremonitor.metricmodels.Series;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AzureMonitorMetricSender {

	private static final Logger log = LoggerFactory.getLogger(AzureMonitorMetricSender.class);


	private JmeterMetric responseTimeMetric = new JmeterMetric();
	private JmeterMetric throughputMetric = new JmeterMetric();
	private JmeterMetric concurrentUsersMetric = new JmeterMetric();

	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
	private String localHost;

	private String SAMPLER_NAME = "SamplerName";
	private String SAMPLER_STATUS = "SamplerStatus";
	private String JMETER_HOST = "JmeterHost";
	private String SUCCESS = "Success";
	private String ERROR = "Error";

	private final Object lock = new Object();
	private HttpPost httpRequest;
	private CloseableHttpAsyncClient httpClient;
	private URL url;
	private String azureMonitorAccessKey;
	private Future<HttpResponse> lastRequest;
	
	ObjectMapper mapper = new ObjectMapper();

	public AzureMonitorMetricSender(String testName) {
		localHost = JMeterUtils.getLocalHostName()+" "+JMeterUtils.getLocalHostIP();

		responseTimeMetric.getData().getBaseData().setMetric("ResponseTime");
		responseTimeMetric.getData().getBaseData().setNamespace(testName);
		responseTimeMetric.getData().getBaseData().getDimNames().add(JMETER_HOST);
		responseTimeMetric.getData().getBaseData().getDimNames().add(SAMPLER_NAME);
		responseTimeMetric.getData().getBaseData().getDimNames().add(SAMPLER_STATUS);

		throughputMetric.getData().getBaseData().setMetric("HitsPerSecond");
		throughputMetric.getData().getBaseData().setNamespace(testName);
		throughputMetric.getData().getBaseData().getDimNames().add(JMETER_HOST);
		throughputMetric.getData().getBaseData().getDimNames().add(SAMPLER_NAME);


		concurrentUsersMetric.getData().getBaseData().setMetric("Users");
		concurrentUsersMetric.getData().getBaseData().setNamespace(testName);
		concurrentUsersMetric.getData().getBaseData().getDimNames().add(JMETER_HOST);


	}


	public void addUserMetric(long timestamp, UserMetric userMetric) throws Exception {
		String timestampStr = sdf.format(new Date(System.currentTimeMillis()));

		concurrentUsersMetric.setTime(timestampStr);

		{
			Series seriesUsers = new Series();
			seriesUsers.getDimValues().add(localHost);
			seriesUsers.setMin(Double.valueOf(userMetric.getMinActiveThreads()));
			seriesUsers.setMax(Double.valueOf(userMetric.getMaxActiveThreads()));
			seriesUsers.setSum(Double.valueOf(userMetric.getMeanActiveThreads()));
			seriesUsers.setCount(1);
			
			concurrentUsersMetric.getData().getBaseData().getSeries().add(seriesUsers);
		}

	}

	public void addResponseTimeMetric(long timestamp, String samplerName, SamplerMetric metric) throws Exception {
		String timestampStr = sdf.format(new Date(timestamp));

		responseTimeMetric.setTime(timestampStr);

		//add success samplers metrics
		if(metric.getSuccesses() > 0)
		{
			Series seriesOK = new Series();
			seriesOK.getDimValues().add(localHost);
			seriesOK.getDimValues().add(samplerName);
			seriesOK.getDimValues().add(SUCCESS);
			seriesOK.setMin(metric.getOkMinTime());
			seriesOK.setMax(metric.getOkMaxTime());
			seriesOK.setSum(metric.getOkMean() * metric.getSuccesses());
			seriesOK.setCount(metric.getSuccesses());

			responseTimeMetric.getData().getBaseData().getSeries().add(seriesOK);
		}

		//add error samplers metrics
		if(metric.getFailures() > 0)
		{
			Series seriesKO = new Series();
			seriesKO.getDimValues().add(localHost);
			seriesKO.getDimValues().add(samplerName);
			seriesKO.getDimValues().add(ERROR);
			seriesKO.setMin(metric.getKoMinTime());
			seriesKO.setMax(metric.getKoMaxTime());
			seriesKO.setSum(metric.getKoMean() * metric.getFailures());
			seriesKO.setCount(metric.getFailures());

			responseTimeMetric.getData().getBaseData().getSeries().add(seriesKO);
		}

	}

	public void addThroughputMetric(long timestamp, String samplerName, Double throughput) throws Exception {
		String timestampStr = sdf.format(new Date(timestamp));

		throughputMetric.setTime(timestampStr);

		//add total hits per sec for the  sampler
		{
			Series seriesHitsPerSec = new Series();
			seriesHitsPerSec.getDimValues().add(localHost);
			seriesHitsPerSec.getDimValues().add(samplerName);
			seriesHitsPerSec.setMin(throughput);
			seriesHitsPerSec.setMax(throughput);
			seriesHitsPerSec.setSum(throughput);
			seriesHitsPerSec.setCount(1);

			throughputMetric.getData().getBaseData().getSeries().add(seriesHitsPerSec);
		}



	}


	/**
	 * @param azureMonitorURL
	 * @throws Exception
	 */
	public void setup(String azureMonitorURL, String azureMonitorAccessKey) throws Exception {
		// Create I/O reactor configuration
		IOReactorConfig ioReactorConfig = IOReactorConfig
				.custom()
				.setIoThreadCount(3)
				.setConnectTimeout(JMeterUtils.getPropDefault("backend_azuremonitor.connection_timeout", 1000))
				.setSoTimeout(JMeterUtils.getPropDefault("backend_azuremonitor.socket_timeout", 3000))
				.build();
		// Create a custom I/O reactor
		ConnectingIOReactor ioReactor = new DefaultConnectingIOReactor(ioReactorConfig);

		// Create a connection manager with custom configuration.
		PoolingNHttpClientConnectionManager connManager = new PoolingNHttpClientConnectionManager(
				ioReactor);

		httpClient = HttpAsyncClientBuilder.create()
				.setConnectionManager(connManager)
				.setMaxConnPerRoute(5)
				.setMaxConnTotal(5)
				.setUserAgent("ApacheJMeter"+JMeterUtils.getJMeterVersion())
				.disableCookieManagement()
				.disableConnectionState()
				.build();
		this.url = new URL(azureMonitorURL);
		this.azureMonitorAccessKey = azureMonitorAccessKey;
		httpRequest = createRequest();
		
		httpClient.start();
	}

	/**
	 * @return {@link HttpPost}
	 * @throws URISyntaxException 
	 */
	private HttpPost createRequest() throws URISyntaxException {
		RequestConfig defaultRequestConfig = RequestConfig.custom()
				.setConnectTimeout(JMeterUtils.getPropDefault("backend_azuremonitor.connection_timeout", 10000))
				.setSocketTimeout(JMeterUtils.getPropDefault("backend_azuremonitor.socket_timeout", 30000))
				.setConnectionRequestTimeout(JMeterUtils.getPropDefault("backend_azuremonitor.connection_request_timeout", 30000))
				.build();

		HttpPost currentHttpRequest = new HttpPost(url.toURI());
		currentHttpRequest.setConfig(defaultRequestConfig);
		currentHttpRequest.addHeader("Authorization", "Bearer "+azureMonitorAccessKey);
		
		log.info("Created AzureMonitorMetricSender with url: {}", url);
		return currentHttpRequest;
	}
	
	public void sendMetrics() {
		try {
			sendMetrics(responseTimeMetric);
		}catch(Exception e) {
			log.error("Error while sending responseTimeMetric to AzureMonitor.", e);
		}
		
		try {
			sendMetrics(concurrentUsersMetric);
		}catch(Exception e) {
			log.error("Error while sending concurentUserMetric to AzureMonitor.", e);
		}
		
		
		try {
			sendMetrics(throughputMetric);
		}catch(Exception e) {
			log.error("Error while sending throughputMetric to AzureMonitor.", e);
		}
	}
	
	public void sendMetrics(final JmeterMetric jmeterMetric) throws Exception{
		String metricData = null;
		final int metricCount = jmeterMetric.getData().getBaseData().getSeries().size();
		
		synchronized (lock) {
            if(jmeterMetric.getData().getBaseData().getSeries().isEmpty()) {
                return;
            }
            metricData = mapper.writeValueAsString(jmeterMetric);
            jmeterMetric.getData().getBaseData().getSeries().clear();            
        }
		
		try {
			if(httpRequest == null) {
                httpRequest = createRequest();
            }
			
			if(metricCount > 0) {
				httpRequest.setEntity(new StringEntity(metricData, ContentType.APPLICATION_JSON));
				log.debug("Azure Monitor metrics: " + metricData);
				lastRequest = httpClient.execute(httpRequest, new FutureCallback<HttpResponse>() {
	                @Override
	                public void completed(final HttpResponse response) {
	                    int code = response.getStatusLine().getStatusCode();
	                    if(code == 200) {
	                            log.info("Success, number of '"+jmeterMetric.getData().getBaseData().getMetric()+"' metrics written: {}", metricCount);
	                    } else {
	                            log.error("Error writing '"+jmeterMetric.getData().getBaseData().getMetric()+"' metrics to Azure Monitor Url: {}, responseCode: {}", url, code);
	                    }
	                    
	                }
	                @Override
	                public void failed(final Exception ex) {
	                    log.error("Failed to send metric '"+jmeterMetric.getData().getBaseData().getMetric()+"' to Azure Monitor server.", ex);
	                }
	                @Override
	                public void cancelled() {
	                    log.warn("Request to Azure Monitor server was cancelled.");
	                }
	            });      
				
			}
			
		}catch(Exception e) {
			log.error("Error in sendig the metrics '"+jmeterMetric.getData().getBaseData().getMetric()+"' to Azure Monitor.",e);
		}finally {
			//drop the metric
			//tempMetrics.getData().getBaseData().getSeries().clear();
		}
	}
	
	 public void destroy() {
	        // Give some time to send last metrics before shutting down
	        log.info("Destroying AzureMonitorMetricSender..");
	        try {
	        	if(lastRequest != null) {
	        		lastRequest.get(5, TimeUnit.SECONDS);
	        	}
	            
	        } catch (Exception e) {
	            log.error("Error waiting for last request to be send to Azure Monitor", e);
	        }
	        if(httpRequest != null) {
	            httpRequest.abort();
	        }
	        
	        IOUtils.closeQuietly(httpClient);
	    }


}
