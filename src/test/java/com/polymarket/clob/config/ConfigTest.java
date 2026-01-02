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
        assertEquals("0x4bFb41d5B3570DeFd03C39a9A4D8dE6Bd8B8982E", config.getExchange());
        assertEquals("0x2791Bca1f2de4661ED88A30C99A7a9449Aa84174", config.getCollateral());
        assertEquals("0x4D97DCd97eC945f40cF65F87097ACe5EA0476045", config.getConditionalTokens());
    }
    
    @Test
    public void testGetContractConfigAmoy() {
        ContractConfig config = Config.getContractConfig(AMOY);
        assertNotNull(config);
        assertEquals("0xdFE02Eb6733538f8Ea35D585af8DE5958AD99E40", config.getExchange());
    }
    
    @Test
    public void testGetContractConfigNegRisk() {
        ContractConfig config = Config.getContractConfig(POLYGON, true);
        assertNotNull(config);
        assertEquals("0xC5d563A36AE78145C45a50134d48A1215220f80a", config.getExchange());
    }
    
    @Test
    public void testInvalidChainId() {
        assertThrows(IllegalArgumentException.class, () -> {
            Config.getContractConfig(999999);
        });
    }
}
