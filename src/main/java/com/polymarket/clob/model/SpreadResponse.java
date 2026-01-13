package com.polymarket.clob.model;


import com.alibaba.fastjson2.annotation.JSONField;

/**
 * Response model for spread
 */

public class SpreadResponse {

    @JSONField(name = "spread")
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

