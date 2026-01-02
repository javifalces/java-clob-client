package com.polymarket.clob.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Parameters for querying trades
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeParams {
    private String id;
    private String makerAddress;
    private String market;
    private String assetId;
    private Long before;
    private Long after;
}
