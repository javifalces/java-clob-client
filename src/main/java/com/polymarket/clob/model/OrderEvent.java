package com.polymarket.clob.model;


import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents an order event from the WebSocket stream.
 * Contains information about order placements, modifications, and cancellations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class OrderEvent {

    /**
     * Event type, should always be "order" for this event
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
     * Unique order identifier
     */
    @JSONField(name = "id")
    private String id;

    /**
     * The price of the order
     */
    @JSONField(name = "price")
    private String price;

    /**
     * The side of the order (BUY or SELL)
     */
    @JSONField(name = "side")
    private String side;

    /**
     * The original size of the order
     */
    @JSONField(name = "original_size")
    private String originalSize;

    /**
     * The amount that has been matched
     */
    @JSONField(name = "size_matched")
    private String sizeMatched;

    /**
     * The outcome of the order (YES or NO)
     */
    @JSONField(name = "outcome")
    private String outcome;

    /**
     * The owner of the order
     */
    @JSONField(name = "owner")
    private String owner;

    /**
     * The order owner identifier
     */
    @JSONField(name = "order_owner")
    private String orderOwner;

    /**
     * Type of the order event (e.g., PLACEMENT, CANCEL, MATCH)
     */
    @JSONField(name = "type")
    private String type;

    /**
     * Timestamp when the order event occurred (in seconds)
     */
    @JSONField(name = "timestamp")
    private String timestamp;

    /**
     * Associated trades with this order (if any)
     */
    @JSONField(name = "associate_trades")
    private List<String> associateTrades;

    /**
     * Get timestamp as long
     */
    public long getTimestampAsLong() {
        return timestamp != null ? Long.parseLong(timestamp) : 0L;
    }

    /**
     * Get price as double
     */
    public double getPriceAsDouble() {
        return price != null ? Double.parseDouble(price) : 0.0;
    }

    /**
     * Get original size as double
     */
    public double getOriginalSizeAsDouble() {
        return originalSize != null ? Double.parseDouble(originalSize) : 0.0;
    }

    /**
     * Get size matched as double
     */
    public double getSizeMatchedAsDouble() {
        return sizeMatched != null ? Double.parseDouble(sizeMatched) : 0.0;
    }

    /**
     * Get remaining size (original - matched)
     */
    public double getRemainingSize() {
        return getOriginalSizeAsDouble() - getSizeMatchedAsDouble();
    }

    /**
     * Check if this is a placement event
     */
    public boolean isPlacement() {
        return "PLACEMENT".equalsIgnoreCase(type);
    }

    /**
     * Check if this is a cancel event
     */
    public boolean isCancel() {
        return "CANCEL".equalsIgnoreCase(type);
    }

    /**
     * Check if this is a match event
     */
    public boolean isMatch() {
        return "MATCH".equalsIgnoreCase(type);
    }

    /**
     * Check if order is fully matched
     */
    public boolean isFullyMatched() {
        return getSizeMatchedAsDouble() >= getOriginalSizeAsDouble();
    }

    /**
     * Check if this is a buy order
     */
    public boolean isBuy() {
        return "BUY".equalsIgnoreCase(side);
    }

    /**
     * Check if this is a sell order
     */
    public boolean isSell() {
        return "SELL".equalsIgnoreCase(side);
    }
}

