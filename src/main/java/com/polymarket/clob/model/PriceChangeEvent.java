package com.polymarket.clob.model;


import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a price_change event from the WebSocket stream.
 * Contains information about price changes for one or more assets in a market.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class PriceChangeEvent {

    /**
     * The market identifier where the price changes occurred
     */
    @JSONField(name = "market")
    private String market;

    /**
     * List of price changes for different assets
     */
    @JSONField(name = "price_changes")
    private List<PriceChangeEntry> priceChanges;

    /**
     * Timestamp when the price change occurred (in milliseconds)
     */
    @JSONField(name = "timestamp")
    private String timestamp;

    /**
     * Event type, should always be "price_change" for this event
     */
    @JSONField(name = "event_type")
    private String eventType;

    /**
     * Get timestamp as long
     */
    public long getTimestampAsLong() {
        return timestamp != null ? Long.parseLong(timestamp) : 0L;
    }
}

