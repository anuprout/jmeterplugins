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
	"dimValues",
	"min",
	"max",
	"sum",
	"count"
})
public class Series {

	@JsonProperty("dimValues")
	private List<String> dimValues = new ArrayList<String>();
	@JsonProperty("min")
	private Double min;
	@JsonProperty("max")
	private Double max;
	@JsonProperty("sum")
	private Double sum;
	@JsonProperty("count")
	private Integer count;
	@JsonIgnore
	private Map<String, Object> additionalProperties = new HashMap<String, Object>();

	@JsonProperty("dimValues")
	public List<String> getDimValues() {
		return dimValues;
	}

	@JsonProperty("dimValues")
	public void setDimValues(List<String> dimValues) {
		this.dimValues = dimValues;
	}

	@JsonProperty("min")
	public Double getMin() {
		return min;
	}

	@JsonProperty("min")
	public void setMin(Double min) {
		this.min = min;
	}

	@JsonProperty("max")
	public Double getMax() {
		return max;
	}

	@JsonProperty("max")
	public void setMax(Double d) {
		this.max = d;
	}

	@JsonProperty("sum")
	public Double getSum() {
		return sum;
	}

	@JsonProperty("sum")
	public void setSum(Double sum) {
		this.sum = sum;
	}

	@JsonProperty("count")
	public Integer getCount() {
		return count;
	}

	@JsonProperty("count")
	public void setCount(Integer count) {
		this.count = count;
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
