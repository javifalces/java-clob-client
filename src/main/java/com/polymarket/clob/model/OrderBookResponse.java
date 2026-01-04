package com.polymarket.clob.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;

/**
 * Response model for order book data
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderBookResponse {

    @JsonProperty("timestamp")
    private String timestamp;

    @JsonProperty("market")
    private String market;

    @JsonProperty("asset_id")
    private String assetId;

    @JsonProperty("bids")
    private List<Map<String, String>> bids;

    @JsonProperty("asks")
    private List<Map<String, String>> asks;

    @JsonProperty("hash")
    private String hash;

    public OrderBookResponse() {
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getMarket() {
        return market;
    }

    public void setMarket(String market) {
        this.market = market;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public List<Map<String, String>> getBids() {
        return bids;
    }

    public void setBids(List<Map<String, String>> bids) {
        this.bids = bids;
    }

    public List<Map<String, String>> getAsks() {
        return asks;
    }

    public void setAsks(List<Map<String, String>> asks) {
        this.asks = asks;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    /**
     * Get the best bid price
     */
    public Double getBestBidPrice() {
        if (bids != null && !bids.isEmpty() && bids.get(0) != null) {
            String price = bids.get(0).get("price");
            if (price != null) {
                return Double.parseDouble(price);
            }
        }
        return null;
    }

    /**
     * Get the best bid size
     */
    public Double getBestBidSize() {
        if (bids != null && !bids.isEmpty() && bids.get(0) != null) {
            String size = bids.get(0).get("size");
            if (size != null) {
                return Double.parseDouble(size);
            }
        }
        return null;
    }

    /**
     * Get the best ask price
     */
    public Double getBestAskPrice() {
        if (asks != null && !asks.isEmpty() && asks.get(0) != null) {
            String price = asks.get(0).get("price");
            if (price != null) {
                return Double.parseDouble(price);
            }
        }
        return null;
    }

    /**
     * Get the best ask size
     */
    public Double getBestAskSize() {
        if (asks != null && !asks.isEmpty() && asks.get(0) != null) {
            String size = asks.get(0).get("size");
            if (size != null) {
                return Double.parseDouble(size);
            }
        }
        return null;
    }

    /**
     * Get the spread (best ask - best bid)
     */
    public Double getSpread() {
        Double bestBid = getBestBidPrice();
        Double bestAsk = getBestAskPrice();
        if (bestBid != null && bestAsk != null) {
            return bestAsk - bestBid;
        }
        return null;
    }

    /**
     * Get the mid price
     */
    public Double getMidPrice() {
        Double bestBid = getBestBidPrice();
        Double bestAsk = getBestAskPrice();
        if (bestBid != null && bestAsk != null) {
            return (bestBid + bestAsk) / 2.0;
        }
        return null;
    }

    @Override
    public String toString() {
        return "OrderBookResponse{" +
                "timestamp='" + timestamp + '\'' +
                ", market='" + market + '\'' +
                ", assetId='" + assetId + '\'' +
                ", bids=" + (bids != null ? bids.size() + " levels" : "null") +
                ", asks=" + (asks != null ? asks.size() + " levels" : "null") +
                ", hash='" + hash + '\'' +
                ", bestBid=" + getBestBidPrice() +
                ", bestAsk=" + getBestAskPrice() +
                ", spread=" + getSpread() +
                '}';
    }
}

