package com.polymarket.clob.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response model for spread
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpreadResponse {

    @JsonProperty("spread")
    private String spread;

    public SpreadResponse() {
    }

    public SpreadResponse(String spread) {
        this.spread = spread;
    }

    public String getSpread() {
        return spread;
    }

    public void setSpread(String spread) {
        this.spread = spread;
    }

    public double getSpreadAsDouble() {
        return spread != null ? Double.parseDouble(spread) : 0.0;
    }

    @Override
    public String toString() {
        return "SpreadResponse{" +
                "spread='" + spread + '\'' +
                '}';
    }
}

