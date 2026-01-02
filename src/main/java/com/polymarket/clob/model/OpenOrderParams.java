package com.polymarket.clob.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Parameters for querying open orders
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpenOrderParams {
    private String id;
    private String market;
    private String assetId;
}
