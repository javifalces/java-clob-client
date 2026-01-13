package com.polymarket.clob.model;


import com.alibaba.fastjson2.annotation.JSONField;

/**
 * Response model for last trade price
 */

public class LastTradePriceResponse {

    @JSONField(name = "price")
    private String price;

    public LastTradePriceResponse() {
    }

    public LastTradePriceResponse(String price) {
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
        return "LastTradePriceResponse{" +
                "price='" + price + '\'' +
                '}';
    }
}

