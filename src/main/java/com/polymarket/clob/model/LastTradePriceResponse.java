package com.polymarket.clob.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response model for last trade price
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LastTradePriceResponse {

    @JsonProperty("price")
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

