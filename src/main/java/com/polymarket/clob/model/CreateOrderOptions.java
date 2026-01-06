package com.polymarket.clob.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Options for creating an order
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderOptions {
    /**
     * The tick size for the market
     */
    private String tickSize;

    /**
     * Whether this is a negative risk market
     */
    @Builder.Default
    private boolean negRisk = false;
}

