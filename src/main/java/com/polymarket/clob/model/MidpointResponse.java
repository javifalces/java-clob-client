package com.polymarket.clob.model;


import com.alibaba.fastjson2.annotation.JSONField;

/**
 * Response model for midpoint price
 */

public class MidpointResponse {

    @JSONField(name = "mid")
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

