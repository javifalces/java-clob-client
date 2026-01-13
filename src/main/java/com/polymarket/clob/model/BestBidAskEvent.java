package com.polymarket.clob.model;


import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a best_bid_ask event from the WebSocket stream.
 * Contains the current best bid and ask prices with the spread.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class BestBidAskEvent {

    /**
     * Event type, should always be "best_bid_ask" for this event
     */
    @JSONField(name = "event_type")
    private String eventType;

    /**
     * The market identifier
     */
    @JSONField(name = "market")
    private String market;

    /**
     * The unique identifier for the asset
     */
    @JSONField(name = "asset_id")
    private String assetId;

    /**
     * Best bid price (highest buy price)
     */
    @JSONField(name = "best_bid")
    private String bestBid;

    /**
     * Best ask price (lowest sell price)
     */
    @JSONField(name = "best_ask")
    private String bestAsk;

    /**
     * The spread between best bid and best ask
     */
    @JSONField(name = "spread")
    private String spread;

    /**
     * Timestamp when the update occurred (in milliseconds)
     */
    @JSONField(name = "timestamp")
    private String timestamp;

    /**
     * Get timestamp as long
     */
    public long getTimestampAsLong() {
        return timestamp != null ? Long.parseLong(timestamp) : 0L;
    }

    /**
     * Get best bid as double
     */
    public double getBestBidAsDouble() {
        return bestBid != null ? Double.parseDouble(bestBid) : 0.0;
    }

    /**
     * Get best ask as double
     */
    public double getBestAskAsDouble() {
        return bestAsk != null ? Double.parseDouble(bestAsk) : 0.0;
    }

    /**
     * Get spread as double
     */
    public double getSpreadAsDouble() {
        return spread != null ? Double.parseDouble(spread) : 0.0;
    }

    /**
     * Get the mid price (average of best bid and ask)
     */
    public double getMidPrice() {
        return (getBestBidAsDouble() + getBestAskAsDouble()) / 2.0;
    }
}

