package com.polymarket.clob.model;


import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single price change entry for an asset.
 * Contains details about the price update including asset ID, price, size, side, and best bid/ask.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class PriceChangeEntry {

    /**
     * The unique identifier for the asset
     */
    @JSONField(name = "asset_id")
    private String assetId;

    /**
     * The new price for this asset
     */
    @JSONField(name = "price")
    private String price;

    /**
     * The size/volume at this price level
     */
    @JSONField(name = "size")
    private String size;

    /**
     * The side of the order book (BUY or SELL)
     */
    @JSONField(name = "side")
    private String side;

    /**
     * Hash of the order or transaction
     */
    @JSONField(name = "hash")
    private String hash;

    /**
     * Best bid price in the order book
     */
    @JSONField(name = "best_bid")
    private String bestBid;

    /**
     * Best ask price in the order book
     */
    @JSONField(name = "best_ask")
    private String bestAsk;

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
}

