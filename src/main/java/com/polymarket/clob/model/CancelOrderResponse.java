package com.polymarket.clob.model;


import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Response returned when canceling orders
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancelOrderResponse {

    /**
     * List of order IDs that were successfully canceled
     */
    @JSONField(name = "canceled")
    private List<String> canceled;

    /**
     * Map of order IDs that were not canceled with their error details
     */
    @JSONField(name = "not_canceled")
    private Map<String, Object> notCanceled;

    /**
     * Check if any orders were successfully canceled
     */
    public boolean hasAnyCanceled() {
        return canceled != null && !canceled.isEmpty();
    }

    /**
     * Check if any orders failed to cancel
     */
    public boolean hasAnyNotCanceled() {
        return notCanceled != null && !notCanceled.isEmpty();
    }
}

