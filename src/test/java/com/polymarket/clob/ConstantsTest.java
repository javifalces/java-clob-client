package com.polymarket.clob;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Constants class
 */
public class ConstantsTest {
    
    @Test
    public void testConstants() {
        assertEquals(0, Constants.L0);
        assertEquals(1, Constants.L1);
        assertEquals(2, Constants.L2);
        assertEquals("0x0000000000000000000000000000000000000000", Constants.ZERO_ADDRESS);
        assertEquals(80002, Constants.AMOY);
        assertEquals(137, Constants.POLYGON);
        assertEquals("LTE=", Constants.END_CURSOR);
    }
    
    @Test
    public void testAuthMessages() {
        assertNotNull(Constants.L1_AUTH_UNAVAILABLE);
        assertNotNull(Constants.L2_AUTH_UNAVAILABLE);
        assertNotNull(Constants.CREDENTIAL_CREATION_WARNING);
    }
}
