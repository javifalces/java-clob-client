package com.polymarket.clob.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Parameters for querying open orders via GET /data/orders on clob.polymarket.com.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpenOrderParams {
    /**
     * Filter by order ID (hash)
     */
    private String id;
    /** Filter by market (condition ID) */
    private String market;
    /** Filter by asset ID (token ID) */
    private String assetId;
}
