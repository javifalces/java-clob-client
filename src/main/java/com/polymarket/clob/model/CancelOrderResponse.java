package com.polymarket.clob.model;


import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response returned when canceling an order
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class CancelOrderResponse {

    /**
     * The order ID that was canceled
     */
    @JSONField(name = "orderID")
    private String orderId;

    /**
     * Whether the cancellation was successful
     */
    @JSONField(name = "success")
    private Boolean success;

    /**
     * Error message if the cancellation failed
     */
    @JSONField(name = "errorMsg")
    private String errorMsg;

    /**
     * Status of the order after cancellation
     */
    @JSONField(name = "status")
    private String status;

    /**
     * Transaction hash if applicable
     */
    @JSONField(name = "transactionsHash")
    private String transactionsHash;

    /**
     * Check if the cancellation was successful
     */
    public boolean isSuccessful() {
        return success != null && success;
    }
}

