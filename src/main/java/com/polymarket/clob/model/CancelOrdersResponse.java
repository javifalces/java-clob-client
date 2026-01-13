package com.polymarket.clob.model;


import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response returned when canceling multiple orders
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class CancelOrdersResponse {

    /**
     * List of order IDs that were successfully canceled
     */
    @JSONField(name = "canceled")
    private List<String> canceled;

    /**
     * List of order IDs that failed to cancel
     */
    @JSONField(name = "failed")
    private List<String> failed;

    /**
     * Whether the overall operation was successful
     */
    @JSONField(name = "success")
    private Boolean success;

    /**
     * Error message if any
     */
    @JSONField(name = "errorMsg")
    private String errorMsg;

    /**
     * Check if all orders were successfully canceled
     */
    public boolean isAllSuccessful() {
        return success != null && success && (failed == null || failed.isEmpty());
    }

    /**
     * Get the number of successfully canceled orders
     */
    public int getCanceledCount() {
        return canceled != null ? canceled.size() : 0;
    }

    /**
     * Get the number of failed cancellations
     */
    public int getFailedCount() {
        return failed != null ? failed.size() : 0;
    }
}

