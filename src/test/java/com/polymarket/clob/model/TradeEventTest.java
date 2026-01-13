package com.polymarket.clob.model;

import com.alibaba.fastjson2.JSON;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TradeEvent model deserialization
 */
public class TradeEventTest {


    /**
     * Tests that a trade event JSON can be deserialized correctly.
     */
    @Test
    void testTradeEventDeserialization() throws Exception {
        String json = """
                    {
                      "asset_id": "52114319501245915516055106046884209969926127482827954674443846427813813222426",
                      "event_type": "trade",
                      "id": "28c4d2eb-bbea-40e7-a9f0-b2fdb56b2c2e",
                      "last_update": "1672290701",
                      "maker_orders": [
                        {
                          "asset_id": "52114319501245915516055106046884209969926127482827954674443846427813813222426",
                          "matched_amount": "10",
                          "order_id": "0xff354cd7ca7539dfa9c28d90943ab5779a4eac34b9b37a757d7b32bdfb11790b",
                          "outcome": "YES",
                          "owner": "9180014b-33c8-9240-a14b-bdca11c0a465",
                          "price": "0.57"
                        }
                      ],
                      "market": "0xbd31dc8a20211944f6b70f31557f1001557b59905b7738480ca09bd4532f84af",
                      "matchtime": "1672290701",
                      "outcome": "YES",
                      "owner": "9180014b-33c8-9240-a14b-bdca11c0a465",
                      "price": "0.57",
                      "side": "BUY",
                      "size": "10",
                      "status": "MATCHED",
                      "taker_order_id": "0x06bc63e346ed4ceddce9efd6b3af37c8f8f440c92fe7da6b2d0f9e4ccbc50c42",
                      "timestamp": "1672290701",
                      "trade_owner": "9180014b-33c8-9240-a14b-bdca11c0a465",
                      "type": "TRADE"
                    }
                """;

        TradeEvent event = JSON.parseObject(json, TradeEvent.class);

        // Verify top-level fields
        assertNotNull(event);
        assertEquals("trade", event.getEventType());
        assertEquals("52114319501245915516055106046884209969926127482827954674443846427813813222426", event.getAssetId());
        assertEquals("0xbd31dc8a20211944f6b70f31557f1001557b59905b7738480ca09bd4532f84af", event.getMarket());
        assertEquals("28c4d2eb-bbea-40e7-a9f0-b2fdb56b2c2e", event.getId());
        assertEquals("0.57", event.getPrice());
        assertEquals("BUY", event.getSide());
        assertEquals("10", event.getSize());
        assertEquals("YES", event.getOutcome());
        assertEquals("9180014b-33c8-9240-a14b-bdca11c0a465", event.getOwner());
        assertEquals("9180014b-33c8-9240-a14b-bdca11c0a465", event.getTradeOwner());
        assertEquals("0x06bc63e346ed4ceddce9efd6b3af37c8f8f440c92fe7da6b2d0f9e4ccbc50c42", event.getTakerOrderId());
        assertEquals("MATCHED", event.getStatus());
        assertEquals("TRADE", event.getType());
        assertEquals("1672290701", event.getTimestamp());
        assertEquals("1672290701", event.getMatchtime());
        assertEquals("1672290701", event.getLastUpdate());

        // Test conversion methods
        assertEquals(0.57, event.getPriceAsDouble(), 0.001);
        assertEquals(10.0, event.getSizeAsDouble(), 0.001);
        assertEquals(1672290701L, event.getTimestampAsLong());
        assertEquals(1672290701L, event.getMatchtimeAsLong());
        assertEquals(1672290701L, event.getLastUpdateAsLong());
        assertEquals(5.7, event.getTradeValue(), 0.001);

        // Test side checkers
        assertTrue(event.isBuy());
        assertFalse(event.isSell());

        // Verify maker orders
        List<MakerOrder> makerOrders = event.getMakerOrders();
        assertNotNull(makerOrders);
        assertEquals(1, makerOrders.size());

        MakerOrder makerOrder = makerOrders.get(0);
        assertEquals("52114319501245915516055106046884209969926127482827954674443846427813813222426", makerOrder.getAssetId());
        assertEquals("10", makerOrder.getMatchedAmount());
        assertEquals("0xff354cd7ca7539dfa9c28d90943ab5779a4eac34b9b37a757d7b32bdfb11790b", makerOrder.getOrderId());
        assertEquals("YES", makerOrder.getOutcome());
        assertEquals("9180014b-33c8-9240-a14b-bdca11c0a465", makerOrder.getOwner());
        assertEquals("0.57", makerOrder.getPrice());
        assertEquals(10.0, makerOrder.getMatchedAmountAsDouble(), 0.001);
        assertEquals(0.57, makerOrder.getPriceAsDouble(), 0.001);
    }

    /**
     * Tests trade event with SELL side.
     */
    @Test
    void testTradeEventSellSide() throws Exception {
        String json = """
                    {
                      "asset_id": "test-asset",
                      "event_type": "trade",
                      "id": "test-id",
                      "market": "test-market",
                      "price": "0.45",
                      "side": "SELL",
                      "size": "50",
                      "outcome": "NO",
                      "owner": "test-owner",
                      "status": "MATCHED",
                      "type": "TRADE",
                      "timestamp": "1234567890"
                    }
                """;

        TradeEvent event = JSON.parseObject(json, TradeEvent.class);

        assertEquals("SELL", event.getSide());
        assertTrue(event.isSell());
        assertFalse(event.isBuy());
    }

    /**
     * Tests that TradeEvent handles null values gracefully.
     */
    @Test
    void testTradeEventWithNullValues() {
        TradeEvent event = new TradeEvent();

        assertEquals(0.0, event.getPriceAsDouble());
        assertEquals(0.0, event.getSizeAsDouble());
        assertEquals(0L, event.getTimestampAsLong());
        assertEquals(0.0, event.getTradeValue());
        assertFalse(event.isBuy());
        assertFalse(event.isSell());
    }

    /**
     * Tests TradeEvent builder pattern.
     */
    @Test
    void testTradeEventBuilder() {
        MakerOrder makerOrder = MakerOrder.builder()
                .assetId("test-asset")
                .matchedAmount("25")
                .orderId("test-order")
                .outcome("YES")
                .owner("test-owner")
                .price("0.75")
                .build();

        TradeEvent event = TradeEvent.builder()
                .eventType("trade")
                .assetId("test-asset")
                .market("test-market")
                .id("test-id")
                .price("0.75")
                .side("BUY")
                .size("25")
                .outcome("YES")
                .owner("test-owner")
                .makerOrders(List.of(makerOrder))
                .status("MATCHED")
                .type("TRADE")
                .timestamp("123456789")
                .build();

        assertEquals("trade", event.getEventType());
        assertEquals("test-asset", event.getAssetId());
        assertEquals(0.75, event.getPriceAsDouble(), 0.001);
        assertEquals(25.0, event.getSizeAsDouble(), 0.001);
        assertEquals(18.75, event.getTradeValue(), 0.001);
        assertTrue(event.isBuy());
        assertEquals(1, event.getMakerOrders().size());
    }

    /**
     * Tests trade value calculation.
     */
    @Test
    void testTradeValueCalculation() {
        TradeEvent event1 = TradeEvent.builder()
                .price("0.5")
                .size("100")
                .build();
        assertEquals(50.0, event1.getTradeValue(), 0.001);

        TradeEvent event2 = TradeEvent.builder()
                .price("0.99")
                .size("500")
                .build();
        assertEquals(495.0, event2.getTradeValue(), 0.001);
    }
}

