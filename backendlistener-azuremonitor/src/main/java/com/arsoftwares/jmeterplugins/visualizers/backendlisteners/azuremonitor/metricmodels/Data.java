package com.arsoftwares.jmeterplugins.visualizers.backendlisteners.azuremonitor.metricmodels;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;




//@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
	"baseData"
})
public class Data {

	@JsonProperty("baseData")
	private BaseData baseData = new BaseData();

	@JsonIgnore
	private Map<String, Object> additionalProperties = new HashMap<String, Object>();

	@JsonProperty("baseData")
	public BaseData getBaseData() {
		return baseData;
	}

	@JsonProperty("baseData")
	public void setBaseData(BaseData baseData) {
		this.baseData = baseData;
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