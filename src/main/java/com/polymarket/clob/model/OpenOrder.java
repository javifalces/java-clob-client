package com.polymarket.clob.model;


import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents an open order object returned from the getOrder API endpoint.
 * Contains full details about an order including its status, size, and matching information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpenOrder {

    /**
     * Any Trade id the order has been partially included in
     */
    @JSONField(name = "associate_trades")
    private List<String> associateTrades;

    /**
     * Order id
     */
    @JSONField(name = "id")
    private String id;

    /**
     * Order current status
     */
    @JSONField(name = "status")
    private String status;

    /**
     * Market id (condition id)
     */
    @JSONField(name = "market")
    private String market;

    /**
     * Original order size at placement
     */
    @JSONField(name = "original_size")
    private String originalSize;

    /**
     * Human readable outcome the order is for
     */
    @JSONField(name = "outcome")
    private String outcome;

    /**
     * Maker address (funder)
     */
    @JSONField(name = "maker_address")
    private String makerAddress;

    /**
     * API key
     */
    @JSONField(name = "owner")
    private String owner;

    /**
     * Price
     */
    @JSONField(name = "price")
    private String price;

    /**
     * Side (buy or sell)
     */
    @JSONField(name = "side")
    private String side;

    /**
     * Size of order that has been matched/filled
     */
    @JSONField(name = "size_matched")
    private String sizeMatched;

    /**
     * Token id
     */
    @JSONField(name = "asset_id")
    private String assetId;

    /**
     * Unix timestamp when the order expired, 0 if it does not expire
     */
    @JSONField(name = "expiration")
    private String expiration;

    /**
     * Order type (GTC, FOK, GTD)
     */
    @JSONField(name = "type")
    private String type;

    /**
     * Unix timestamp when the order was created
     */
    @JSONField(name = "created_at")
    private String createdAt;

    /**
     * Get original size as double
     */
    public double getOriginalSizeAsDouble() {
        return originalSize != null ? Double.parseDouble(originalSize) : 0.0;
    }

    /**
     * Get price as double
     */
    public double getPriceAsDouble() {
        return price != null ? Double.parseDouble(price) : 0.0;
    }

    /**
     * Get size matched as double
     */
    public double getSizeMatchedAsDouble() {
        return sizeMatched != null ? Double.parseDouble(sizeMatched) : 0.0;
    }

    /**
     * Get expiration as long
     */
    public long getExpirationAsLong() {
        return expiration != null ? Long.parseLong(expiration) : 0L;
    }

    /**
     * Get created at as long
     */
    public long getCreatedAtAsLong() {
        return createdAt != null ? Long.parseLong(createdAt) : 0L;
    }

    /**
     * Get remaining size (original size - size matched)
     */
    public double getRemainingSize() {
        return getOriginalSizeAsDouble() - getSizeMatchedAsDouble();
    }

    /**
     * Get fill percentage (size matched / original size * 100)
     */
    public double getFillPercentage() {
        double original = getOriginalSizeAsDouble();
        if (original == 0.0) return 0.0;
        return (getSizeMatchedAsDouble() / original) * 100.0;
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

    /**
     * Check if the order is fully filled
     */
    public boolean isFullyFilled() {
        return getOriginalSizeAsDouble() == getSizeMatchedAsDouble();
    }

    /**
     * Check if the order is partially filled
     */
    public boolean isPartiallyFilled() {
        double matched = getSizeMatchedAsDouble();
        return matched > 0 && matched < getOriginalSizeAsDouble();
    }

    /**
     * Check if the order has expired
     */
    public boolean isExpired() {
        long exp = getExpirationAsLong();
        if (exp == 0) return false; // Never expires
        return System.currentTimeMillis() / 1000 > exp;
    }
}
