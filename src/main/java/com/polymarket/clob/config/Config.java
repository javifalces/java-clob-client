package com.polymarket.clob.config;

import com.polymarket.clob.model.ContractConfig;
import java.util.HashMap;
import java.util.Map;

import static com.polymarket.clob.Constants.AMOY;
import static com.polymarket.clob.Constants.POLYGON;

/**
 * Configuration manager for contract addresses
 */
public class Config {
    
    private static final Map<Integer, ContractConfig> CONFIGS = new HashMap<>();
    private static final Map<Integer, ContractConfig> NEG_RISK_CONFIGS = new HashMap<>();
    
    static {
        // Standard configs
        CONFIGS.put(POLYGON, new ContractConfig(
            "0x4bFb41d5B3570DeFd03C39a9A4D8dE6Bd8B8982E",
            "0x2791Bca1f2de4661ED88A30C99A7a9449Aa84174",
            "0x4D97DCd97eC945f40cF65F87097ACe5EA0476045"
        ));
        
        CONFIGS.put(AMOY, new ContractConfig(
            "0xdFE02Eb6733538f8Ea35D585af8DE5958AD99E40",
            "0x9c4e1703476e875070ee25b56a58b008cfb8fa78",
            "0x69308FB512518e39F9b16112fA8d994F4e2Bf8bB"
        ));
        
        // Negative risk configs
        NEG_RISK_CONFIGS.put(POLYGON, new ContractConfig(
            "0xC5d563A36AE78145C45a50134d48A1215220f80a",
            "0x2791bca1f2de4661ed88a30c99a7a9449aa84174",
            "0x4D97DCd97eC945f40cF65F87097ACe5EA0476045"
        ));
        
        NEG_RISK_CONFIGS.put(AMOY, new ContractConfig(
            "0xd91E80cF2E7be2e162c6513ceD06f1dD0dA35296",
            "0x9c4e1703476e875070ee25b56a58b008cfb8fa78",
            "0x69308FB512518e39F9b16112fA8d994F4e2Bf8bB"
        ));
    }
    
    /**
     * Get the contract configuration for a specific chain
     * 
     * @param chainId The chain ID
     * @param negRisk Whether to use negative risk configuration
     * @return ContractConfig for the chain
     * @throws IllegalArgumentException if chainId is invalid
     */
    public static ContractConfig getContractConfig(int chainId, boolean negRisk) {
        Map<Integer, ContractConfig> configMap = negRisk ? NEG_RISK_CONFIGS : CONFIGS;
        ContractConfig config = configMap.get(chainId);
        
        if (config == null) {
            throw new IllegalArgumentException("Invalid chainID: " + chainId);
        }
        
        return config;
    }
    
    /**
     * Get the contract configuration for a specific chain (standard, non-negative risk)
     * 
     * @param chainId The chain ID
     * @return ContractConfig for the chain
     */
    public static ContractConfig getContractConfig(int chainId) {
        return getContractConfig(chainId, false);
    }
}
