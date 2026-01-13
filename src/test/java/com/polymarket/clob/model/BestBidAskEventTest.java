package com.polymarket.clob.model;

import com.alibaba.fastjson2.JSON;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BestBidAskEvent model deserialization
 */
public class BestBidAskEventTest {


    /**
     * Tests that a best_bid_ask event JSON can be deserialized correctly.
     */
    @Test
    void testBestBidAskEventDeserialization() throws Exception {
        String json = """
                    {
                      "event_type": "best_bid_ask",
                      "market": "0x0005c0d312de0be897668695bae9f32b624b4a1ae8b140c49f08447fcc74f442",
                      "asset_id": "85354956062430465315924116860125388538595433819574542752031640332592237464430",
                      "best_bid": "0.73",
                      "best_ask": "0.77",
                      "spread": "0.04",
                      "timestamp": "1766789469958"
                    }
                """;

        BestBidAskEvent event = JSON.parseObject(json, BestBidAskEvent.class);

        // Verify all fields
        assertNotNull(event);
        assertEquals("best_bid_ask", event.getEventType());
        assertEquals("0x0005c0d312de0be897668695bae9f32b624b4a1ae8b140c49f08447fcc74f442", event.getMarket());
        assertEquals("85354956062430465315924116860125388538595433819574542752031640332592237464430", event.getAssetId());
        assertEquals("0.73", event.getBestBid());
        assertEquals("0.77", event.getBestAsk());
        assertEquals("0.04", event.getSpread());
        assertEquals("1766789469958", event.getTimestamp());

        // Test conversion methods
        assertEquals(0.73, event.getBestBidAsDouble(), 0.001);
        assertEquals(0.77, event.getBestAskAsDouble(), 0.001);
        assertEquals(0.04, event.getSpreadAsDouble(), 0.001);
        assertEquals(1766789469958L, event.getTimestampAsLong());

        // Test mid price calculation
        assertEquals(0.75, event.getMidPrice(), 0.001);
    }

    /**
     * Tests that BestBidAskEvent handles null values gracefully.
     */
    @Test
    void testBestBidAskEventWithNullValues() {
        BestBidAskEvent event = new BestBidAskEvent();

        assertEquals(0.0, event.getBestBidAsDouble());
        assertEquals(0.0, event.getBestAskAsDouble());
        assertEquals(0.0, event.getSpreadAsDouble());
        assertEquals(0L, event.getTimestampAsLong());
        assertEquals(0.0, event.getMidPrice());
    }

    /**
     * Tests BestBidAskEvent builder pattern.
     */
    @Test
    void testBestBidAskEventBuilder() {
        BestBidAskEvent event = BestBidAskEvent.builder()
                .eventType("best_bid_ask")
                .market("test-market")
                .assetId("test-asset")
                .bestBid("0.50")
                .bestAsk("0.52")
                .spread("0.02")
                .timestamp("123456789")
                .build();

        assertEquals("best_bid_ask", event.getEventType());
        assertEquals("test-market", event.getMarket());
        assertEquals("test-asset", event.getAssetId());
        assertEquals(0.50, event.getBestBidAsDouble(), 0.001);
        assertEquals(0.52, event.getBestAskAsDouble(), 0.001);
        assertEquals(0.02, event.getSpreadAsDouble(), 0.001);
        assertEquals(0.51, event.getMidPrice(), 0.001);
    }

    /**
     * Tests mid price calculation with various bid/ask values.
     */
    @Test
    void testMidPriceCalculation() {
        BestBidAskEvent event1 = BestBidAskEvent.builder()
                .bestBid("1.00")
                .bestAsk("2.00")
                .build();
        assertEquals(1.50, event1.getMidPrice(), 0.001);

        BestBidAskEvent event2 = BestBidAskEvent.builder()
                .bestBid("0.99")
                .bestAsk("1.01")
                .build();
        assertEquals(1.00, event2.getMidPrice(), 0.001);

        BestBidAskEvent event3 = BestBidAskEvent.builder()
                .bestBid("0.456")
                .bestAsk("0.789")
                .build();
        assertEquals(0.6225, event3.getMidPrice(), 0.0001);
    }

    /**
     * Tests that spread calculation matches expected value.
     */
    @Test
    void testSpreadValidation() throws Exception {
        String json = """
                    {
                      "event_type": "best_bid_ask",
                      "market": "test-market",
                      "asset_id": "test-asset",
                      "best_bid": "0.50",
                      "best_ask": "0.55",
                      "spread": "0.05",
                      "timestamp": "123456789"
                    }
                """;

        BestBidAskEvent event = JSON.parseObject(json, BestBidAskEvent.class);

        // Verify spread matches the difference
        double calculatedSpread = event.getBestAskAsDouble() - event.getBestBidAsDouble();
        assertEquals(event.getSpreadAsDouble(), calculatedSpread, 0.001);
    }
}

