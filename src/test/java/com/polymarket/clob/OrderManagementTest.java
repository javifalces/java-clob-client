package com.polymarket.clob;

import com.polymarket.clob.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for order creation and posting methods
 * <p>
 * Note: These tests require valid credentials and network access.
 * They are disabled by default to prevent accidental API calls.
 */
public class OrderManagementTest {

    private ClobClient client;
    private static final String TEST_HOST = "https://clob.polymarket.com";
    private static final int TEST_CHAIN_ID = 137; // Polygon mainnet
    private static final String TEST_TOKEN_ID = "your_test_token_id";

    @BeforeEach
    public void setUp() {
        // Create a Level 0 client for demonstration
        // In real tests, you would use a test private key
        client = new ClobClient(TEST_HOST);
    }

    /**
     * Example: Creating and signing an order
     * Requires Level 1 auth (private key)
     */
    // @Test
    public void testCreateOrder() {
        // This test is disabled by default
        // To run it, uncomment and provide valid credentials

        String privateKey = "your_private_key";
        ClobClient authClient = new ClobClient(TEST_HOST, TEST_CHAIN_ID, privateKey);

        OrderArgs orderArgs = OrderArgs.builder()
                .tokenId(TEST_TOKEN_ID)
                .price(0.5)
                .size(10.0)
                .side(Constants.BUY)
                .feeRateBps(0)
                .nonce(0)
                .expiration(0)
                .taker(Constants.ZERO_ADDRESS)
                .build();

        SignedOrder order = authClient.createOrder(orderArgs);

        assertNotNull(order);
        assertNotNull(order.getSignature());
        assertEquals(Constants.BUY, order.getSide());
    }

    /**
     * Example: Creating and posting an order in one step
     * Requires Level 2 auth (API credentials)
     */
    // @Test
    public void testCreateAndPostOrder() {
        // This test is disabled by default
        // To run it, uncomment and provide valid credentials

        String privateKey = "your_private_key";
        ApiCreds creds = new ApiCreds("api_key", "secret", "passphrase");
        ClobClient authClient = new ClobClient(TEST_HOST, TEST_CHAIN_ID, privateKey, creds);

        OrderArgs orderArgs = OrderArgs.builder()
                .tokenId(TEST_TOKEN_ID)
                .price(0.5)
                .size(10.0)
                .side(Constants.BUY)
                .build();

        OrderResponse response = authClient.createAndPostOrder(orderArgs);

        assertNotNull(response);
        assertTrue(response.isSuccessful());
        assertNotNull(response.getOrderId());
    }

    /**
     * Example: Creating a market order
     * Requires Level 1 auth (private key)
     */
    // @Test
    public void testCreateMarketOrder() {
        // This test is disabled by default

        String privateKey = "your_private_key";
        ClobClient authClient = new ClobClient(TEST_HOST, TEST_CHAIN_ID, privateKey);

        MarketOrderArgs orderArgs = MarketOrderArgs.builder()
                .tokenId(TEST_TOKEN_ID)
                .amount(10.0)
                .side(Constants.BUY)
                .price(0.55) // Optional - will be calculated if not provided
                .build();

        SignedOrder order = authClient.createMarketOrder(orderArgs);

        assertNotNull(order);
        assertNotNull(order.getSignature());
    }

    /**
     * Example: Posting multiple orders
     * Requires Level 2 auth (API credentials)
     */
    // @Test
    public void testPostMultipleOrders() {
        // This test is disabled by default

        String privateKey = "your_private_key";
        ApiCreds creds = new ApiCreds("api_key", "secret", "passphrase");
        ClobClient authClient = new ClobClient(TEST_HOST, TEST_CHAIN_ID, privateKey, creds);

        // Create first order
        OrderArgs order1Args = OrderArgs.builder()
                .tokenId(TEST_TOKEN_ID)
                .price(0.5)
                .size(10.0)
                .side(Constants.BUY)
                .build();
        SignedOrder order1 = authClient.createOrder(order1Args);

        // Create second order
        OrderArgs order2Args = OrderArgs.builder()
                .tokenId(TEST_TOKEN_ID)
                .price(0.6)
                .size(5.0)
                .side(Constants.BUY)
                .build();
        SignedOrder order2 = authClient.createOrder(order2Args);

        // Post both orders
        List<OrderResponse> responses = authClient.postOrders(Arrays.asList(
                PostOrdersArgs.builder().order(order1).orderType(OrderType.GTC).build(),
                PostOrdersArgs.builder().order(order2).orderType(OrderType.GTC).build()
        ));

        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertTrue(responses.get(0).isSuccessful());
        assertTrue(responses.get(1).isSuccessful());
    }

    /**
     * Example: Canceling multiple orders
     * Requires Level 2 auth (API credentials)
     */
    // @Test
    public void testCancelMultipleOrders() {
        // This test is disabled by default

        String privateKey = "your_private_key";
        ApiCreds creds = new ApiCreds("api_key", "secret", "passphrase");
        ClobClient authClient = new ClobClient(TEST_HOST, TEST_CHAIN_ID, privateKey, creds);

        CancelOrdersResponse response = authClient.cancelOrders(Arrays.asList(
                "order_id_1",
                "order_id_2"
        ));

        assertNotNull(response);
        assertTrue(response.isAllSuccessful());
    }

    /**
     * Example: Canceling all orders for a market
     * Requires Level 2 auth (API credentials)
     */
    // @Test
    public void testCancelMarketOrders() {
        // This test is disabled by default

        String privateKey = "your_private_key";
        ApiCreds creds = new ApiCreds("api_key", "secret", "passphrase");
        ClobClient authClient = new ClobClient(TEST_HOST, TEST_CHAIN_ID, privateKey, creds);

        CancelOrdersResponse response = authClient.cancelMarketOrders("market_id", null);

        assertNotNull(response);
        System.out.println("Canceled: " + response.getCanceledCount());
        System.out.println("Failed: " + response.getFailedCount());
    }

    @Test
    public void testOrderArgsBuilder() {
        // This test validates the builder pattern works correctly
        OrderArgs orderArgs = OrderArgs.builder()
                .tokenId("test_token")
                .price(0.5)
                .size(10.0)
                .side(Constants.BUY)
                .feeRateBps(10)
                .nonce(1)
                .expiration(System.currentTimeMillis() / 1000 + 3600)
                .taker(Constants.ZERO_ADDRESS)
                .build();

        assertEquals("test_token", orderArgs.getTokenId());
        assertEquals(0.5, orderArgs.getPrice());
        assertEquals(10.0, orderArgs.getSize());
        assertEquals(Constants.BUY, orderArgs.getSide());
        assertEquals(10, orderArgs.getFeeRateBps());
        assertEquals(1, orderArgs.getNonce());
        assertEquals(Constants.ZERO_ADDRESS, orderArgs.getTaker());
    }

    @Test
    public void testMarketOrderArgsBuilder() {
        // This test validates the market order builder pattern
        MarketOrderArgs orderArgs = MarketOrderArgs.builder()
                .tokenId("test_token")
                .amount(100.0)
                .side(Constants.SELL)
                .price(0.45)
                .feeRateBps(10)
                .orderType(OrderType.FOK)
                .build();

        assertEquals("test_token", orderArgs.getTokenId());
        assertEquals(100.0, orderArgs.getAmount());
        assertEquals(Constants.SELL, orderArgs.getSide());
        assertEquals(0.45, orderArgs.getPrice());
        assertEquals(10, orderArgs.getFeeRateBps());
        assertEquals(OrderType.FOK, orderArgs.getOrderType());
    }
}

