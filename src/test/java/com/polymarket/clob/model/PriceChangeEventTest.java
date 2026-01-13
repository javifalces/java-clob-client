package com.polymarket.clob.model;

import com.alibaba.fastjson2.JSON;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PriceChangeEvent model deserialization
 */
public class PriceChangeEventTest {


    /**
     * Tests that a price_change event JSON can be deserialized correctly.
     */
    @Test
    void testPriceChangeEventDeserialization() throws Exception {
        String json = """
                    {
                        "market": "0x17815081230e3b9c78b098162c33b1ffa68c4ec29c123d3d14989599e0c2e113",
                        "price_changes": [
                            {
                                "asset_id": "71478852790279095447182996049071040792010759617668969799049179229104800573786",
                                "price": "0.996",
                                "size": "938331.68",
                                "side": "BUY",
                                "hash": "b7f28a64a2151c216c317f61061a1e4b99f6ee2c",
                                "best_bid": "0.997",
                                "best_ask": "0.998"
                            },
                            {
                                "asset_id": "11862165566757345985240476164489718219056735011698825377388402888080786399275",
                                "price": "0.004",
                                "size": "938331.68",
                                "side": "SELL",
                                "hash": "0269fd6f6f32a20c747ca9edbc22ffe9e65a5fbb",
                                "best_bid": "0.002",
                                "best_ask": "0.003"
                            }
                        ],
                        "timestamp": "1768323366524",
                        "event_type": "price_change"
                    }
                """;

        PriceChangeEvent event = JSON.parseObject(json, PriceChangeEvent.class);

        // Verify top-level fields
        assertNotNull(event);
        assertEquals("0x17815081230e3b9c78b098162c33b1ffa68c4ec29c123d3d14989599e0c2e113", event.getMarket());
        assertEquals("1768323366524", event.getTimestamp());
        assertEquals("price_change", event.getEventType());
        assertEquals(1768323366524L, event.getTimestampAsLong());

        // Verify price changes list
        List<PriceChangeEntry> priceChanges = event.getPriceChanges();
        assertNotNull(priceChanges);
        assertEquals(2, priceChanges.size());

        // Verify first price change (BUY)
        PriceChangeEntry firstChange = priceChanges.get(0);
        assertEquals("71478852790279095447182996049071040792010759617668969799049179229104800573786", firstChange.getAssetId());
        assertEquals("0.996", firstChange.getPrice());
        assertEquals("938331.68", firstChange.getSize());
        assertEquals("BUY", firstChange.getSide());
        assertEquals("b7f28a64a2151c216c317f61061a1e4b99f6ee2c", firstChange.getHash());
        assertEquals("0.997", firstChange.getBestBid());
        assertEquals("0.998", firstChange.getBestAsk());

        // Test conversion methods
        assertEquals(0.996, firstChange.getPriceAsDouble(), 0.001);
        assertEquals(938331.68, firstChange.getSizeAsDouble(), 0.01);
        assertEquals(0.997, firstChange.getBestBidAsDouble(), 0.001);
        assertEquals(0.998, firstChange.getBestAskAsDouble(), 0.001);

        // Verify second price change (SELL)
        PriceChangeEntry secondChange = priceChanges.get(1);
        assertEquals("11862165566757345985240476164489718219056735011698825377388402888080786399275", secondChange.getAssetId());
        assertEquals("0.004", secondChange.getPrice());
        assertEquals("938331.68", secondChange.getSize());
        assertEquals("SELL", secondChange.getSide());
        assertEquals("0269fd6f6f32a20c747ca9edbc22ffe9e65a5fbb", secondChange.getHash());
        assertEquals("0.002", secondChange.getBestBid());
        assertEquals("0.003", secondChange.getBestAsk());

        // Test conversion methods for second change
        assertEquals(0.004, secondChange.getPriceAsDouble(), 0.001);
        assertEquals(938331.68, secondChange.getSizeAsDouble(), 0.01);
        assertEquals(0.002, secondChange.getBestBidAsDouble(), 0.001);
        assertEquals(0.003, secondChange.getBestAskAsDouble(), 0.001);
    }

    /**
     * Tests that PriceChangeEvent can handle empty price_changes list.
     */
    @Test
    void testPriceChangeEventWithEmptyList() throws Exception {
        String json = """
                    {
                        "market": "0x17815081230e3b9c78b098162c33b1ffa68c4ec29c123d3d14989599e0c2e113",
                        "price_changes": [],
                        "timestamp": "1768323366524",
                        "event_type": "price_change"
                    }
                """;

        PriceChangeEvent event = JSON.parseObject(json, PriceChangeEvent.class);

        assertNotNull(event);
        assertEquals("0x17815081230e3b9c78b098162c33b1ffa68c4ec29c123d3d14989599e0c2e113", event.getMarket());
        assertNotNull(event.getPriceChanges());
        assertEquals(0, event.getPriceChanges().size());
    }

    /**
     * Tests that PriceChangeEntry handles null values gracefully.
     */
    @Test
    void testPriceChangeEntryWithNullValues() {
        PriceChangeEntry entry = new PriceChangeEntry();

        assertEquals(0.0, entry.getPriceAsDouble());
        assertEquals(0.0, entry.getSizeAsDouble());
        assertEquals(0.0, entry.getBestBidAsDouble());
        assertEquals(0.0, entry.getBestAskAsDouble());
    }

    /**
     * Tests PriceChangeEvent builder pattern.
     */
    @Test
    void testPriceChangeEventBuilder() {
        PriceChangeEntry entry = PriceChangeEntry.builder()
                .assetId("test-asset")
                .price("0.5")
                .size("1000")
                .side("BUY")
                .hash("test-hash")
                .bestBid("0.49")
                .bestAsk("0.51")
                .build();

        PriceChangeEvent event = PriceChangeEvent.builder()
                .market("test-market")
                .priceChanges(List.of(entry))
                .timestamp("123456789")
                .eventType("price_change")
                .build();

        assertEquals("test-market", event.getMarket());
        assertEquals(1, event.getPriceChanges().size());
        assertEquals("test-asset", event.getPriceChanges().get(0).getAssetId());
    }
}

