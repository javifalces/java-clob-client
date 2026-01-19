package com.polymarket.clob.signing;

import com.polymarket.clob.Constants;
import com.polymarket.clob.model.CreateOrderOptions;
import com.polymarket.clob.model.OrderArgs;
import com.polymarket.clob.model.SignedOrder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for OrderBuilder address normalization
 */
public class OrderBuilderAddressTest {

    private static final String TEST_PRIVATE_KEY = "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef";
    private static final int TEST_CHAIN_ID = 137;

    @Test
    public void testAddressesHave0xPrefix() {
        Signer signer = new Signer(TEST_PRIVATE_KEY, TEST_CHAIN_ID);
        OrderBuilder builder = new OrderBuilder(signer, 0, null);

        OrderArgs orderArgs = OrderArgs.builder()
                .tokenId("123456")
                .price(0.55)
                .size(100.0)
                .side(Constants.BUY)
                .feeRateBps(0)
                .nonce(0)
                .expiration(0)
                .taker(Constants.ZERO_ADDRESS)
                .build();

        CreateOrderOptions options = CreateOrderOptions.builder()
                .tickSize("0.01")
                .negRisk(false)
                .build();

        SignedOrder order = builder.createOrder(orderArgs, options);

        // Verify all addresses have 0x prefix
        assertNotNull(order.getMaker());
        assertTrue(order.getMaker().startsWith("0x"), "Maker address should start with 0x");

        assertNotNull(order.getSigner());
        assertTrue(order.getSigner().startsWith("0x"), "Signer address should start with 0x");

        assertNotNull(order.getTaker());
        assertTrue(order.getTaker().startsWith("0x"), "Taker address should start with 0x");
    }

    @Test
    public void testAddressNormalizationWithAndWithout0x() {
        Signer signer = new Signer(TEST_PRIVATE_KEY, TEST_CHAIN_ID);

        // Test with funder address without 0x prefix
        String funderWithout0x = "1234567890123456789012345678901234567890";
        OrderBuilder builder1 = new OrderBuilder(signer, 0, funderWithout0x);

        OrderArgs orderArgs = OrderArgs.builder()
                .tokenId("123456")
                .price(0.55)
                .size(100.0)
                .side(Constants.BUY)
                .feeRateBps(0)
                .nonce(0)
                .expiration(0)
                .taker("0000000000000000000000000000000000000000") // without 0x
                .build();

        CreateOrderOptions options = CreateOrderOptions.builder()
                .tickSize("0.01")
                .negRisk(false)
                .build();

        SignedOrder order = builder1.createOrder(orderArgs, options);

        // All addresses should have 0x prefix after normalization
        assertTrue(order.getMaker().startsWith("0x"), "Maker should be normalized to have 0x");
        assertTrue(order.getSigner().startsWith("0x"), "Signer should be normalized to have 0x");
        assertTrue(order.getTaker().startsWith("0x"), "Taker should be normalized to have 0x");
    }

    @Test
    public void testSignerAddressHas0xPrefix() {
        Signer signer = new Signer(TEST_PRIVATE_KEY, TEST_CHAIN_ID);
        String address = signer.getAddress();

        assertNotNull(address);
        assertTrue(address.startsWith("0x"), "Signer address should start with 0x");
        assertEquals(42, address.length(), "Ethereum address should be 42 characters (0x + 40 hex chars)");
    }
}

