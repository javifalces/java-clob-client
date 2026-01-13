package com.polymarket.clob.model;

import com.alibaba.fastjson2.JSON;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OrderEvent model deserialization
 */
public class OrderEventTest {


    /**
     * Tests that an order event JSON can be deserialized correctly.
     */
    @Test
    void testOrderEventDeserialization() throws Exception {
        String json = """
                    {
                      "asset_id": "52114319501245915516055106046884209969926127482827954674443846427813813222426",
                      "associate_trades": null,
                      "event_type": "order",
                      "id": "0xff354cd7ca7539dfa9c28d90943ab5779a4eac34b9b37a757d7b32bdfb11790b",
                      "market": "0xbd31dc8a20211944f6b70f31557f1001557b59905b7738480ca09bd4532f84af",
                      "order_owner": "9180014b-33c8-9240-a14b-bdca11c0a465",
                      "original_size": "10",
                      "outcome": "YES",
                      "owner": "9180014b-33c8-9240-a14b-bdca11c0a465",
                      "price": "0.57",
                      "side": "SELL",
                      "size_matched": "0",
                      "timestamp": "1672290687",
                      "type": "PLACEMENT"
                    }
                """;

        OrderEvent event = JSON.parseObject(json, OrderEvent.class);

        // Verify all fields
        assertNotNull(event);
        assertEquals("order", event.getEventType());
        assertEquals("52114319501245915516055106046884209969926127482827954674443846427813813222426", event.getAssetId());
        assertEquals("0xbd31dc8a20211944f6b70f31557f1001557b59905b7738480ca09bd4532f84af", event.getMarket());
        assertEquals("0xff354cd7ca7539dfa9c28d90943ab5779a4eac34b9b37a757d7b32bdfb11790b", event.getId());
        assertEquals("9180014b-33c8-9240-a14b-bdca11c0a465", event.getOrderOwner());
        assertEquals("10", event.getOriginalSize());
        assertEquals("YES", event.getOutcome());
        assertEquals("9180014b-33c8-9240-a14b-bdca11c0a465", event.getOwner());
        assertEquals("0.57", event.getPrice());
        assertEquals("SELL", event.getSide());
        assertEquals("0", event.getSizeMatched());
        assertEquals("1672290687", event.getTimestamp());
        assertEquals("PLACEMENT", event.getType());

        // Test conversion methods
        assertEquals(0.57, event.getPriceAsDouble(), 0.001);
        assertEquals(10.0, event.getOriginalSizeAsDouble(), 0.001);
        assertEquals(0.0, event.getSizeMatchedAsDouble(), 0.001);
        assertEquals(10.0, event.getRemainingSize(), 0.001);
        assertEquals(1672290687L, event.getTimestampAsLong());

        // Test type checkers
        assertTrue(event.isPlacement());
        assertFalse(event.isCancel());
        assertFalse(event.isMatch());
        assertFalse(event.isFullyMatched());

        // Test side checkers
        assertTrue(event.isSell());
        assertFalse(event.isBuy());
    }

    /**
     * Tests order event with BUY side.
     */
    @Test
    void testOrderEventBuySide() throws Exception {
        String json = """
                    {
                      "asset_id": "test-asset",
                      "event_type": "order",
                      "id": "test-id",
                      "market": "test-market",
                      "original_size": "50",
                      "outcome": "NO",
                      "owner": "test-owner",
                      "price": "0.45",
                      "side": "BUY",
                      "size_matched": "25",
                      "timestamp": "1234567890",
                      "type": "MATCH"
                    }
                """;

        OrderEvent event = JSON.parseObject(json, OrderEvent.class);

        assertEquals("BUY", event.getSide());
        assertTrue(event.isBuy());
        assertFalse(event.isSell());
        assertTrue(event.isMatch());
        assertEquals(25.0, event.getRemainingSize(), 0.001);
        assertFalse(event.isFullyMatched());
    }

    /**
     * Tests fully matched order.
     */
    @Test
    void testFullyMatchedOrder() throws Exception {
        String json = """
                    {
                      "asset_id": "test-asset",
                      "event_type": "order",
                      "id": "test-id",
                      "market": "test-market",
                      "original_size": "100",
                      "owner": "test-owner",
                      "price": "0.50",
                      "side": "BUY",
                      "size_matched": "100",
                      "timestamp": "1234567890",
                      "type": "MATCH"
                    }
                """;

        OrderEvent event = JSON.parseObject(json, OrderEvent.class);

        assertTrue(event.isFullyMatched());
        assertEquals(0.0, event.getRemainingSize(), 0.001);
    }

    /**
     * Tests order cancellation event.
     */
    @Test
    void testOrderCancelEvent() {
        OrderEvent event = OrderEvent.builder()
                .eventType("order")
                .type("CANCEL")
                .side("SELL")
                .originalSize("50")
                .sizeMatched("10")
                .build();

        assertTrue(event.isCancel());
        assertFalse(event.isPlacement());
        assertFalse(event.isMatch());
        assertEquals(40.0, event.getRemainingSize(), 0.001);
    }

    /**
     * Tests that OrderEvent handles null values gracefully.
     */
    @Test
    void testOrderEventWithNullValues() {
        OrderEvent event = new OrderEvent();

        assertEquals(0.0, event.getPriceAsDouble());
        assertEquals(0.0, event.getOriginalSizeAsDouble());
        assertEquals(0.0, event.getSizeMatchedAsDouble());
        assertEquals(0.0, event.getRemainingSize());
        assertEquals(0L, event.getTimestampAsLong());
        assertFalse(event.isPlacement());
        assertFalse(event.isCancel());
        assertFalse(event.isMatch());
        assertFalse(event.isBuy());
        assertFalse(event.isSell());
    }

    /**
     * Tests OrderEvent builder pattern.
     */
    @Test
    void testOrderEventBuilder() {
        OrderEvent event = OrderEvent.builder()
                .eventType("order")
                .assetId("test-asset")
                .market("test-market")
                .id("test-id")
                .price("0.75")
                .side("BUY")
                .originalSize("100")
                .sizeMatched("50")
                .outcome("YES")
                .owner("test-owner")
                .type("PLACEMENT")
                .timestamp("123456789")
                .build();

        assertEquals("order", event.getEventType());
        assertEquals("test-asset", event.getAssetId());
        assertEquals(0.75, event.getPriceAsDouble(), 0.001);
        assertEquals(100.0, event.getOriginalSizeAsDouble(), 0.001);
        assertEquals(50.0, event.getSizeMatchedAsDouble(), 0.001);
        assertEquals(50.0, event.getRemainingSize(), 0.001);
        assertTrue(event.isPlacement());
        assertTrue(event.isBuy());
    }

    /**
     * Tests remaining size calculation with various scenarios.
     */
    @Test
    void testRemainingSizeCalculation() {
        OrderEvent event1 = OrderEvent.builder()
                .originalSize("100")
                .sizeMatched("0")
                .build();
        assertEquals(100.0, event1.getRemainingSize(), 0.001);

        OrderEvent event2 = OrderEvent.builder()
                .originalSize("100")
                .sizeMatched("75")
                .build();
        assertEquals(25.0, event2.getRemainingSize(), 0.001);

        OrderEvent event3 = OrderEvent.builder()
                .originalSize("100")
                .sizeMatched("100")
                .build();
        assertEquals(0.0, event3.getRemainingSize(), 0.001);
    }
}

