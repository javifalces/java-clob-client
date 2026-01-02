package com.polymarket.clob.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration for contract addresses on a specific chain
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractConfig {
    /**
     * Exchange contract address
     */
    private String exchange;
    
    /**
     * Collateral token address
     */
    private String collateral;
    
    /**
     * Conditional tokens contract address
     */
    private String conditionalTokens;
}
