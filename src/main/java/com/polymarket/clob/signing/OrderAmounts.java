package com.polymarket.clob.signing;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Holds the calculated maker and taker amounts for an order
 */
@Data
@AllArgsConstructor
public class OrderAmounts {
    /**
     * The maker amount in base units (10^6)
     */
    private long makerAmount;

    /**
     * The taker amount in base units (10^6)
     */
    private long takerAmount;
}

