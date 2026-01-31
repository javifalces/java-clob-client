package com.polymarket.clob.model;


import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response wrapper for getTrades API endpoint.
 * Contains a list of trades and pagination cursor.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradesResponse {

    /**
     * List of trades
     */
    @JSONField(name = "data")
    private List<Trade> data;

    /**
     * Pagination cursor for fetching next page
     */
    @JSONField(name = "next_cursor")
    private String nextCursor;
}
