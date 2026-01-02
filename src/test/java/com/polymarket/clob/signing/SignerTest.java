package com.polymarket.clob.signing;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Signer class
 */
public class SignerTest {
    
    // Test private key (DO NOT use in production!)
    private static final String TEST_PRIVATE_KEY = "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef";
    private static final int TEST_CHAIN_ID = 137;
    
    @Test
    public void testSignerCreation() {
        Signer signer = new Signer(TEST_PRIVATE_KEY, TEST_CHAIN_ID);
        assertNotNull(signer);
        assertNotNull(signer.getAddress());
        assertEquals(TEST_CHAIN_ID, signer.getChainId());
    }
    
    @Test
    public void testSignerWithoutPrefix() {
        String keyWithoutPrefix = TEST_PRIVATE_KEY.substring(2);
        Signer signer = new Signer(keyWithoutPrefix, TEST_CHAIN_ID);
        assertNotNull(signer);
        assertNotNull(signer.getAddress());
    }
    
    @Test
    public void testInvalidSigner() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Signer(null, TEST_CHAIN_ID);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            new Signer(TEST_PRIVATE_KEY, 0);
        });
    }
    
    @Test
    public void testSign() {
        Signer signer = new Signer(TEST_PRIVATE_KEY, TEST_CHAIN_ID);
        byte[] testHash = new byte[32]; // Mock hash
        String signature = signer.sign(testHash);
        assertNotNull(signature);
        assertTrue(signature.startsWith("0x"));
        assertEquals(132, signature.length()); // 0x + 130 hex chars (65 bytes)
    }
}
