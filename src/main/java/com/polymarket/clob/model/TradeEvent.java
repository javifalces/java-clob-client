package com.polymarket.clob.model;


import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a trade event from the WebSocket stream.
 * Contains information about a completed trade including maker orders, price, size, and participants.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class TradeEvent {

    /**
     * Event type, should always be "trade" for this event
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
     * Unique trade identifier
     */
    @JSONField(name = "id")
    private String id;

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
     * The outcome of the trade (YES or NO)
     */
    @JSONField(name = "outcome")
    private String outcome;

    /**
     * The owner of the trade
     */
    @JSONField(name = "owner")
    private String owner;

    /**
     * The trade owner identifier
     */
    @JSONField(name = "trade_owner")
    private String tradeOwner;

    /**
     * The taker order ID
     */
    @JSONField(name = "taker_order_id")
    private String takerOrderId;

    /**
     * List of maker orders that were matched
     */
    @JSONField(name = "maker_orders")
    private List<MakerOrder> makerOrders;

    /**
     * Status of the trade (e.g., MATCHED)
     */
    @JSONField(name = "status")
    private String status;

    /**
     * Type of the event (e.g., TRADE)
     */
    @JSONField(name = "type")
    private String type;

    /**
     * Timestamp when the trade occurred (in seconds)
     */
    @JSONField(name = "timestamp")
    private String timestamp;

    /**
     * Match time of the trade (in seconds)
     */
    @JSONField(name = "matchtime")
    private String matchtime;

    /**
     * Last update timestamp (in seconds)
     */
    @JSONField(name = "last_update")
    private String lastUpdate;

    /**
     * Get timestamp as long
     */
    public long getTimestampAsLong() {
        return timestamp != null ? Long.parseLong(timestamp) : 0L;
    }

    /**
     * Get matchtime as long
     */
    public long getMatchtimeAsLong() {
        return matchtime != null ? Long.parseLong(matchtime) : 0L;
    }

    /**
     * Get last update as long
     */
    public long getLastUpdateAsLong() {
        return lastUpdate != null ? Long.parseLong(lastUpdate) : 0L;
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
     * Get total trade value (price * size)
     */
    public double getTradeValue() {
        return getPriceAsDouble() * getSizeAsDouble();
    }

    /**
     * Check if this is a buy trade
     */
    public boolean isBuy() {
        return "BUY".equalsIgnoreCase(side);
    }

    /**
     * Check if this is a sell trade
     */
    public boolean isSell() {
        return "SELL".equalsIgnoreCase(side);
    }
}

