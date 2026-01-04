package com.polymarket.clob.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a single entry in the order book (price and size)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderBookEntry {

    @JsonProperty("price")
    private String price;

    @JsonProperty("size")
    private String size;

    public OrderBookEntry() {
    }

    public OrderBookEntry(String price, String size) {
        this.price = price;
        this.size = size;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public double getPriceAsDouble() {
        return price != null ? Double.parseDouble(price) : 0.0;
    }

    public double getSizeAsDouble() {
        return size != null ? Double.parseDouble(size) : 0.0;
    }

    @Override
    public String toString() {
        return "OrderBookEntry{" +
                "price='" + price + '\'' +
                ", size='" + size + '\'' +
                '}';
    }
}

