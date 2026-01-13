package com.polymarket.clob.model;


import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a last_trade_price event from the WebSocket stream.
 * Contains information about the most recent trade execution.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class LastTradePriceEvent {

    /**
     * Event type, should always be "last_trade_price" for this event
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
     * The price at which the trade executed
     */
    @JSONField(name = "price")
    private String price;

    /**
     * The side of the trade (BUY or SELL)
     */
    @JSONField(name = "side")
    private String side;

    /**
     * The size/volume of the trade
     */
    @JSONField(name = "size")
    private String size;

    /**
     * Fee rate in basis points
     */
    @JSONField(name = "fee_rate_bps")
    private String feeRateBps;

    /**
     * Timestamp when the trade occurred (in milliseconds)
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
     * Get price as double
     */
    public double getPriceAsDouble() {
        return price != null ? Double.parseDouble(price) : 0.0;
    }

    /**
     * Get size as double
     */
    public double getSizeAsDouble() {
        return size != null ? Double.parseDouble(size) : 0.0;
    }

    /**
     * Get fee rate as integer
     */
    public int getFeeRateBpsAsInt() {
        return feeRateBps != null ? Integer.parseInt(feeRateBps) : 0;
    }
}

