package com.arsoftwares.jmeterplugins.visualizers.backendlisteners.azuremonitor.metricmodels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;




/**
 * @author anuprout
 *
 */
@JsonPropertyOrder({
	"metric",
	"namespace",
	"dimNames",
	"series"
})
public class BaseData {

	@JsonProperty("metric")
	private String metric;
	@JsonProperty("namespace")
	private String namespace;
	@JsonProperty("dimNames")
	private List<String> dimNames = new ArrayList<String>();
	@JsonProperty("series")
	private List<Series> series = new ArrayList<Series>();
	@JsonIgnore
	private Map<String, Object> additionalProperties = new HashMap<String, Object>();

	@JsonProperty("metric")
	public String getMetric() {
		return metric;
	}

	@JsonProperty("metric")
	public void setMetric(String metric) {
		this.metric = metric;
	}

	@JsonProperty("namespace")
	public String getNamespace() {
		return namespace;
	}

	@JsonProperty("namespace")
	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	@JsonProperty("dimNames")
	public List<String> getDimNames() {
		return dimNames;
	}

	@JsonProperty("dimNames")
	public void setDimNames(List<String> dimNames) {
		this.dimNames = dimNames;
	}

	@JsonProperty("series")
	public List<Series> getSeries() {
		return series;
	}

	@JsonProperty("series")
	public void setSeries(List<Series> series) {
		this.series = series;
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
