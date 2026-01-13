package com.polymarket.clob.model;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a book event from the WebSocket stream.
 * Contains the full order book snapshot with bids and asks for an asset.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookEvent {

    /**
     * Event type, should always be "book" for this event
     */
    @JSONField(name = "event_type")
    private String eventType;

    /**
     * The unique identifier for the asset
     */
    @JSONField(name = "asset_id")
    private String assetId;

    /**
     * The market identifier
     */
    @JSONField(name = "market")
    private String market;

    /**
     * List of bid orders (buy orders)
     */
    @JSONField(name = "bids")
    private List<OrderBookEntry> bids;

    /**
     * List of ask orders (sell orders)
     */
    @JSONField(name = "asks")
    private List<OrderBookEntry> asks;

    /**
     * Timestamp when the book update occurred (in milliseconds)
     */
    @JSONField(name = "timestamp")
    private String timestamp;

    /**
     * Hash of the book state
     */
    @JSONField(name = "hash")
    private String hash;

    /**
     * Get timestamp as long
     */
    public long getTimestampAsLong() {
        return timestamp != null ? Long.parseLong(timestamp) : 0L;
    }

    /**
     * Get the best bid (highest price on buy side)
     */
    public OrderBookEntry getBestBid() {
        return bids != null && !bids.isEmpty() ? bids.get(0) : null;
    }

    /**
     * Get the best ask (lowest price on sell side)
     */
    public OrderBookEntry getBestAsk() {
        return asks != null && !asks.isEmpty() ? asks.get(0) : null;
    }
}

