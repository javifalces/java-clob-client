package com.polymarket.clob.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderResponse {

    /**
     * The unique order ID assigned by the exchange
     */
    @JsonProperty("orderID")
    private String orderId;

    /**
     * Whether the order was successfully posted
     */
    @JsonProperty("success")
    private Boolean success;

    /**
     * Error message if the order failed
     */
    @JsonProperty("errorMsg")
    private String errorMsg;

    /**
     * Status of the order (e.g., LIVE, MATCHED, etc.)
     */
    @JsonProperty("status")
    private String status;

    /**
     * The order type (GTC, FOK, etc.)
     */
    @JsonProperty("orderType")
    private String orderType;

    /**
     * The order details as submitted
     */
    @JsonProperty("order")
    private Object order;

    /**
     * Transaction hash if applicable
     */
    @JsonProperty("transactionsHash")
    private String transactionsHash;

    /**
     * Timestamp when the order was created
     */
    @JsonProperty("created")
    private String created;

    /**
     * Last update timestamp
     */
    @JsonProperty("last_updated")
    private String lastUpdated;

    /**
     * Check if the order was successfully posted
     */
    public boolean isSuccessful() {
        return success != null && success;
    }
}

