package com.polymarket.clob.model;


import com.alibaba.fastjson2.annotation.JSONField;

/**
 * Response model for price query (by side)
 */

public class PriceResponse {

    @JSONField(name = "price")
    private String price;

    public PriceResponse() {
    }

    public PriceResponse(String price) {
        this.price = price;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public double getPriceAsDouble() {
        return price != null ? Double.parseDouble(price) : 0.0;
    }

    @Override
    public String toString() {
        return "PriceResponse{" +
                "price='" + price + '\'' +
                '}';
    }
}

