package com.polymarket.clob.model;


import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response returned when posting an order to the exchange
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class OrderResponse {

    /**
     * The unique order ID assigned by the exchange
     */
    @JSONField(name = "orderID")
    private String orderId;

    /**
     * Whether the order was successfully posted
     */
    @JSONField(name = "success")
    private Boolean success;

    /**
     * Error message if the order failed
     */
    @JSONField(name = "errorMsg")
    private String errorMsg;

    /**
     * Status of the order (e.g., LIVE, MATCHED, etc.)
     */
    @JSONField(name = "status")
    private String status;

    /**
     * The order type (GTC, FOK, etc.)
     */
    @JSONField(name = "orderType")
    private String orderType;

    /**
     * The order details as submitted
     */
    @JSONField(name = "order")
    private Object order;

    /**
     * Transaction hash if applicable
     */
    @JSONField(name = "transactionsHash")
    private String transactionsHash;

    /**
     * Timestamp when the order was created
     */
    @JSONField(name = "created")
    private String created;

    /**
     * Last update timestamp
     */
    @JSONField(name = "last_updated")
    private String lastUpdated;

    /**
     * Check if the order was successfully posted
     */
    public boolean isSuccessful() {
        return success != null && success;
    }
}

