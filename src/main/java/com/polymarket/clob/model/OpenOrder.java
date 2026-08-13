package com.polymarket.clob.model;


import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents an open order returned from GET /data/orders on clob.polymarket.com.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpenOrder {

    @JSONField(name = "associate_trades")
    private List<String> associateTrades;

    @JSONField(name = "id")
    private String id;

    @JSONField(name = "status")
    private String status;

    @JSONField(name = "market")
    private String market;

    @JSONField(name = "original_size")
    private String originalSize;

    @JSONField(name = "outcome")
    private String outcome;

    @JSONField(name = "maker_address")
    private String makerAddress;

    @JSONField(name = "owner")
    private String owner;

    @JSONField(name = "price")
    private String price;

    @JSONField(name = "side")
    private String side;

    @JSONField(name = "size_matched")
    private String sizeMatched;

    @JSONField(name = "asset_id")
    private String assetId;

    @JSONField(name = "expiration")
    private String expiration;

    @JSONField(name = "order_type")
    private String type;

    @JSONField(name = "created_at")
    private String createdAt;

    public double getOriginalSizeAsDouble() {
        return originalSize != null ? Double.parseDouble(originalSize) : 0.0;
    }

    public double getPriceAsDouble() {
        return price != null ? Double.parseDouble(price) : 0.0;
    }

    public double getSizeMatchedAsDouble() {
        return sizeMatched != null ? Double.parseDouble(sizeMatched) : 0.0;
    }

    public long getExpirationAsLong() {
        return expiration != null ? Long.parseLong(expiration) : 0L;
    }

    public long getCreatedAtAsLong() {
        return createdAt != null ? Long.parseLong(createdAt) : 0L;
    }

    public double getRemainingSize() {
        return getOriginalSizeAsDouble() - getSizeMatchedAsDouble();
    }

    public double getFillPercentage() {
        double original = getOriginalSizeAsDouble();
        if (original == 0.0) return 0.0;
        return (getSizeMatchedAsDouble() / original) * 100.0;
    }

    public boolean isBuy() {
        return "BUY".equalsIgnoreCase(side);
    }

    public boolean isSell() {
        return "SELL".equalsIgnoreCase(side);
    }

    public boolean isFullyFilled() {
        return getOriginalSizeAsDouble() == getSizeMatchedAsDouble();
    }

    public boolean isPartiallyFilled() {
        double matched = getSizeMatchedAsDouble();
        return matched > 0 && matched < getOriginalSizeAsDouble();
    }

    public boolean isExpired() {
        long exp = getExpirationAsLong();
        if (exp == 0) return false;
        return System.currentTimeMillis() / 1000 > exp;
    }
}
