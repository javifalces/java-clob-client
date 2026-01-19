package com.polymarket.clob.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Rounding configuration based on tick size
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoundConfig {
    /**
     * Decimal places for price
     */
    private int price;

    /**
     * Decimal places for size
     */
    private int size;

    /**
     * Decimal places for amount
     */
    private int amount;
}

