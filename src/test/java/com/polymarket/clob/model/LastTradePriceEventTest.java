package com.polymarket.clob.model;

import com.alibaba.fastjson2.JSON;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LastTradePriceEvent model deserialization
 */
public class LastTradePriceEventTest {


    /**
     * Tests that a last_trade_price event JSON can be deserialized correctly.
     */
    @Test
    void testLastTradePriceEventDeserialization() throws Exception {
        String json = """
                    {
                        "asset_id":"114122071509644379678018727908709560226618148003371446110114509806601493071694",
                        "event_type":"last_trade_price",
                        "fee_rate_bps":"0",
                        "market":"0x6a67b9d828d53862160e470329ffea5246f338ecfffdf2cab45211ec578b0347",
                        "price":"0.456",
                        "side":"BUY",
                        "size":"219.217767",
                        "timestamp":"1750428146322"
                    }
                """;

        LastTradePriceEvent event = JSON.parseObject(json, LastTradePriceEvent.class);

        // Verify all fields
        assertNotNull(event);
        assertEquals("last_trade_price", event.getEventType());
        assertEquals("114122071509644379678018727908709560226618148003371446110114509806601493071694", event.getAssetId());
        assertEquals("0x6a67b9d828d53862160e470329ffea5246f338ecfffdf2cab45211ec578b0347", event.getMarket());
        assertEquals("0.456", event.getPrice());
        assertEquals("BUY", event.getSide());
        assertEquals("219.217767", event.getSize());
        assertEquals("0", event.getFeeRateBps());
        assertEquals("1750428146322", event.getTimestamp());

        // Test conversion methods
        assertEquals(0.456, event.getPriceAsDouble(), 0.001);
        assertEquals(219.217767, event.getSizeAsDouble(), 0.000001);
        assertEquals(0, event.getFeeRateBpsAsInt());
        assertEquals(1750428146322L, event.getTimestampAsLong());
    }

    /**
     * Tests that LastTradePriceEvent handles null values gracefully.
     */
    @Test
    void testLastTradePriceEventWithNullValues() {
        LastTradePriceEvent event = new LastTradePriceEvent();

        assertEquals(0.0, event.getPriceAsDouble());
        assertEquals(0.0, event.getSizeAsDouble());
        assertEquals(0, event.getFeeRateBpsAsInt());
        assertEquals(0L, event.getTimestampAsLong());
    }

    /**
     * Tests LastTradePriceEvent builder pattern.
     */
    @Test
    void testLastTradePriceEventBuilder() {
        LastTradePriceEvent event = LastTradePriceEvent.builder()
                .eventType("last_trade_price")
                .assetId("test-asset")
                .market("test-market")
                .price("1.23")
                .side("SELL")
                .size("100")
                .feeRateBps("10")
                .timestamp("123456789")
                .build();

        assertEquals("last_trade_price", event.getEventType());
        assertEquals("test-asset", event.getAssetId());
        assertEquals("test-market", event.getMarket());
        assertEquals(1.23, event.getPriceAsDouble(), 0.001);
        assertEquals("SELL", event.getSide());
        assertEquals(100.0, event.getSizeAsDouble(), 0.001);
        assertEquals(10, event.getFeeRateBpsAsInt());
    }

    /**
     * Tests LastTradePriceEvent with different sides.
     */
    @Test
    void testLastTradePriceEventSides() throws Exception {
        String buyJson = """
                    {
                        "asset_id":"test-asset",
                        "event_type":"last_trade_price",
                        "fee_rate_bps":"5",
                        "market":"test-market",
                        "price":"0.5",
                        "side":"BUY",
                        "size":"100",
                        "timestamp":"123456789"
                    }
                """;

        LastTradePriceEvent buyEvent = JSON.parseObject(buyJson, LastTradePriceEvent.class);
        assertEquals("BUY", buyEvent.getSide());

        String sellJson = buyJson.replace("\"BUY\"", "\"SELL\"");
        LastTradePriceEvent sellEvent = JSON.parseObject(sellJson, LastTradePriceEvent.class);
        assertEquals("SELL", sellEvent.getSide());
    }
}

