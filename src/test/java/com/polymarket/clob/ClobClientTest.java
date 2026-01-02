package com.polymarket.clob;

import com.polymarket.clob.exception.PolyException;
import com.polymarket.clob.model.ApiCreds;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ClobClient class
 */
public class ClobClientTest {
    
    private static final String TEST_HOST = "https://clob.polymarket.com";
    private static final String TEST_PRIVATE_KEY = "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef";
    private static final int TEST_CHAIN_ID = 137;
    
    @Test
    public void testLevel0Client() {
        ClobClient client = new ClobClient(TEST_HOST);
        assertNotNull(client);
        assertEquals(Constants.L0, client.getMode());
        assertNull(client.getAddress());
    }
    
    @Test
    public void testLevel1Client() {
        ClobClient client = new ClobClient(TEST_HOST, TEST_CHAIN_ID, TEST_PRIVATE_KEY);
        assertNotNull(client);
        assertEquals(Constants.L1, client.getMode());
        assertNotNull(client.getAddress());
        assertNotNull(client.getSigner());
    }
    
    @Test
    public void testLevel2Client() {
        ApiCreds creds = new ApiCreds("test-key", "test-secret", "test-passphrase");
        ClobClient client = new ClobClient(TEST_HOST, TEST_CHAIN_ID, TEST_PRIVATE_KEY, creds);
        assertNotNull(client);
        assertEquals(Constants.L2, client.getMode());
        assertNotNull(client.getAddress());
        assertNotNull(client.getCreds());
    }
    
    @Test
    public void testHostNormalization() {
        ClobClient client1 = new ClobClient(TEST_HOST + "/");
        assertEquals(TEST_HOST, client1.getHost());
        
        ClobClient client2 = new ClobClient(TEST_HOST);
        assertEquals(TEST_HOST, client2.getHost());
    }
    
    @Test
    public void testContractAddresses() {
        ClobClient client = new ClobClient(TEST_HOST, TEST_CHAIN_ID, TEST_PRIVATE_KEY);
        
        assertNotNull(client.getCollateralAddress());
        assertNotNull(client.getConditionalAddress());
        assertNotNull(client.getExchangeAddress());
        assertNotNull(client.getExchangeAddress(false));
        assertNotNull(client.getExchangeAddress(true));
    }
    
    @Test
    public void testSetApiCreds() {
        ClobClient client = new ClobClient(TEST_HOST, TEST_CHAIN_ID, TEST_PRIVATE_KEY);
        assertEquals(Constants.L1, client.getMode());
        
        ApiCreds creds = new ApiCreds("test-key", "test-secret", "test-passphrase");
        client.setApiCreds(creds);
        assertEquals(Constants.L2, client.getMode());
    }
}
