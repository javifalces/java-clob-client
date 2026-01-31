package com.polymarket.clob.model;


import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a trade object returned from the getTrades API endpoint.
 * Contains full details about a trade including taker and maker information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trade {

    /**
     * Trade id
     */
    @JSONField(name = "id")
    private String id;

    /**
     * Hash of taker order (market order) that catalyzed the trade
     */
    @JSONField(name = "taker_order_id")
    private String takerOrderId;

    /**
     * Market id (condition id)
     */
    @JSONField(name = "market")
    private String market;

    /**
     * Asset id (token id) of taker order (market order)
     */
    @JSONField(name = "asset_id")
    private String assetId;

    /**
     * Side of the trade (buy or sell)
     */
    @JSONField(name = "side")
    private String side;

    /**
     * Size of the trade
     */
    @JSONField(name = "size")
    private String size;

    /**
     * The fees paid for the taker order expressed in basic points
     */
    @JSONField(name = "fee_rate_bps")
    private String feeRateBps;

    /**
     * Limit price of taker order
     */
    @JSONField(name = "price")
    private String price;

    /**
     * Trade status
     */
    @JSONField(name = "status")
    private String status;

    /**
     * Time at which the trade was matched
     */
    @JSONField(name = "match_time")
    private String matchTime;

    /**
     * Timestamp of last status update
     */
    @JSONField(name = "last_update")
    private String lastUpdate;

    /**
     * Human readable outcome of the trade
     */
    @JSONField(name = "outcome")
    private String outcome;

    /**
     * Funder address of the taker of the trade
     */
    @JSONField(name = "maker_address")
    private String makerAddress;

    /**
     * API key of taker of the trade
     */
    @JSONField(name = "owner")
    private String owner;

    /**
     * Hash of the transaction where the trade was executed
     */
    @JSONField(name = "transaction_hash")
    private String transactionHash;

    /**
     * Index of bucket for trade in case trade is executed in multiple transactions
     */
    @JSONField(name = "bucket_index")
    private Integer bucketIndex;

    /**
     * List of the maker trades the taker trade was filled against
     */
    @JSONField(name = "maker_orders")
    private List<MakerOrder> makerOrders;

    /**
     * Side of the trade: TAKER or MAKER
     */
    @JSONField(name = "type")
    private String type;

    /**
     * Get size as double
     */
    public double getSizeAsDouble() {
        return size != null ? Double.parseDouble(size) : 0.0;
    }

    /**
     * Get price as double
     */
    public double getPriceAsDouble() {
        return price != null ? Double.parseDouble(price) : 0.0;
    }

    /**
     * Get fee rate as double
     */
    public double getFeeRateBpsAsDouble() {
        return feeRateBps != null ? Double.parseDouble(feeRateBps) : 0.0;
    }

    /**
     * Get match time as long
     */
    public long getMatchTimeAsLong() {
        return matchTime != null ? Long.parseLong(matchTime) : 0L;
    }

    /**
     * Get last update as long
     */
    public long getLastUpdateAsLong() {
        return lastUpdate != null ? Long.parseLong(lastUpdate) : 0L;
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

    /**
     * Check if this is a taker trade
     */
    public boolean isTaker() {
        return "TAKER".equalsIgnoreCase(type);
    }

    /**
     * Check if this is a maker trade
     */
    public boolean isMaker() {
        return "MAKER".equalsIgnoreCase(type);
    }
}
