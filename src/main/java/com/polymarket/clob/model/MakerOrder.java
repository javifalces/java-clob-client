package com.polymarket.clob.model;


import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single maker order in a trade event.
 * Contains details about the matched maker order including price, amount, and owner.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class MakerOrder {

    /**
     * The unique identifier for the asset
     */
    @JSONField(name = "asset_id")
    private String assetId;

    /**
     * The amount that was matched in this maker order
     */
    @JSONField(name = "matched_amount")
    private String matchedAmount;

    /**
     * The unique identifier for this maker order
     */
    @JSONField(name = "order_id")
    private String orderId;

    /**
     * The outcome of the order (YES or NO)
     */
    @JSONField(name = "outcome")
    private String outcome;

    /**
     * The owner of the maker order
     */
    @JSONField(name = "owner")
    private String owner;

    /**
     * The price of the maker order
     */
    @JSONField(name = "price")
    private String price;

    /**
     * Get matched amount as double
     */
    public double getMatchedAmountAsDouble() {
        return matchedAmount != null ? Double.parseDouble(matchedAmount) : 0.0;
    }

    /**
     * Get price as double
     */
    public double getPriceAsDouble() {
        return price != null ? Double.parseDouble(price) : 0.0;
    }
}

