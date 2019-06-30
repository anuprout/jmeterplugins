package com.arsoftwares.jmeterplugins.visualizers.backendlisteners.azuremonitor.metricmodels;


import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;





/**
 * This is a POJO representation of JSON data that will be sent to Azure Monitor . Azure Monitor accepts below format data as a custom metric.
 * @see <a href=https://docs.microsoft.com/en-us/azure/azure-monitor/platform/metrics-store-custom-rest-api > Azure Monitor custom metric </a>
 *  
 *
 * @author anuprout
 * 
 *  
 */
@JsonPropertyOrder({
	"time",
	"data"
})
public class JmeterMetric {
	
	@JsonProperty("time")
	private String time;

	@JsonProperty("data")
	private Data data = new Data();

	@JsonIgnore
	private Map<String, Object> additionalProperties = new HashMap<String, Object>();

	@JsonProperty("time")
	public String getTime() {
		return time;
	}

	@JsonProperty("time")
	public void setTime(String time) {
		this.time = time;
	}

	@JsonProperty("data")
	public Data getData() {
		return data;
	}

	@JsonProperty("data")
	public void setData(Data data) {
		this.data = data;
	}

	@JsonAnyGetter
	public Map<String, Object> getAdditionalProperties() {
		return this.additionalProperties;
	}

	@JsonAnySetter
	public void setAdditionalProperty(String name, Object value) {
		this.additionalProperties.put(name, value);
	}

}

