package com.polymarket.clob.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response model for midpoint price
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MidpointResponse {

    @JsonProperty("mid")
    private String mid;

    public MidpointResponse() {
    }

    public MidpointResponse(String mid) {
        this.mid = mid;
    }

    public String getMid() {
        return mid;
    }

    public void setMid(String mid) {
        this.mid = mid;
    }

    public double getMidAsDouble() {
        return mid != null ? Double.parseDouble(mid) : 0.0;
    }

    @Override
    public String toString() {
        return "MidpointResponse{" +
                "mid='" + mid + '\'' +
                '}';
    }
}

