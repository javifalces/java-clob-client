package com.polymarket.clob.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration for contract addresses on a specific chain
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractConfig {
    /**
     * V1 exchange contract address
     */
    private String exchange;

    /**
     * Neg-risk adapter contract address
     */
    private String negRiskAdapter;

    /**
     * V1 neg-risk exchange contract address
     */
    private String negRiskExchange;

    /**
     * Collateral token address
     */
    private String collateral;
    
    /**
     * Conditional tokens contract address
     */
    private String conditionalTokens;

    /**
     * V2 exchange contract address
     */
    private String exchangeV2;

    /**
     * V2 neg-risk exchange contract address
     */
    private String negRiskExchangeV2;
}
