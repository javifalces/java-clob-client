package com.polymarket.clob.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response model for price query (by side)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PriceResponse {

    @JsonProperty("price")
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

