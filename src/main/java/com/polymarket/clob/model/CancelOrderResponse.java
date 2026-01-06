package com.polymarket.clob.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class CancelOrderResponse {

    /**
     * The order ID that was canceled
     */
    @JsonProperty("orderID")
    private String orderId;

    /**
     * Whether the cancellation was successful
     */
    @JsonProperty("success")
    private Boolean success;

    /**
     * Error message if the cancellation failed
     */
    @JsonProperty("errorMsg")
    private String errorMsg;

    /**
     * Status of the order after cancellation
     */
    @JsonProperty("status")
    private String status;

    /**
     * Transaction hash if applicable
     */
    @JsonProperty("transactionsHash")
    private String transactionsHash;

    /**
     * Check if the cancellation was successful
     */
    public boolean isSuccessful() {
        return success != null && success;
    }
}

