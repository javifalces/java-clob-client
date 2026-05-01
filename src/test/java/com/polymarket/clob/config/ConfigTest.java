package com.polymarket.clob.config;

import com.polymarket.clob.model.ContractConfig;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.polymarket.clob.Constants.*;

/**
 * Tests for Config class
 */
public class ConfigTest {
    
    @Test
    public void testGetContractConfigPolygon() {
        ContractConfig config = Config.getContractConfig(POLYGON);
        assertNotNull(config);
        // V1 exchange address
        assertEquals("0x4bFb41d5B3570DeFd03C39a9A4D8dE6Bd8B8982E", config.getExchange());
        // Updated collateral address (matching Python v2 reference)
        assertEquals("0xC011a7E12a19f7B1f670d46F03B03f3342E82DFB", config.getCollateral());
        assertEquals("0x4D97DCd97eC945f40cF65F87097ACe5EA0476045", config.getConditionalTokens());
        // V2 exchange address
        assertEquals("0xE111180000d2663C0091e4f400237545B87B996B", config.getExchangeV2());
        assertEquals("0xe2222d279d744050d28e00520010520000310F59", config.getNegRiskExchangeV2());
    }
    
    @Test
    public void testGetContractConfigAmoy() {
        ContractConfig config = Config.getContractConfig(AMOY);
        assertNotNull(config);
        assertEquals("0xdFE02Eb6733538f8Ea35D585af8DE5958AD99E40", config.getExchange());
        assertEquals("0xE111180000d2663C0091e4f400237545B87B996B", config.getExchangeV2());
    }
    
    @Test
    public void testGetContractConfigNegRisk() {
        ContractConfig config = Config.getContractConfig(POLYGON, true);
        assertNotNull(config);
        // For neg-risk, the same config is returned, but neg-risk-specific addresses apply
        assertEquals("0xC5d563A36AE78145C45a50134d48A1215220f80a", config.getNegRiskExchange());
        assertEquals("0xe2222d279d744050d28e00520010520000310F59", config.getNegRiskExchangeV2());
    }
    
    @Test
    public void testInvalidChainId() {
        assertThrows(IllegalArgumentException.class, () -> {
            Config.getContractConfig(999999);
        });
    }
}
