package com.polymarket.clob.model;

import com.alibaba.fastjson2.JSON;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BookEvent model deserialization
 */
public class BookEventTest {


    /**
     * Tests that a book event JSON can be deserialized correctly.
     */
    @Test
    void testBookEventDeserialization() throws Exception {
        String json = """
                    {
                      "event_type": "book",
                      "asset_id": "65818619657568813474341868652308942079804919287380422192892211131408793125422",
                      "market": "0xbd31dc8a20211944f6b70f31557f1001557b59905b7738480ca09bd4532f84af",
                      "bids": [
                        { "price": ".48", "size": "30" },
                        { "price": ".49", "size": "20" },
                        { "price": ".50", "size": "15" }
                      ],
                      "asks": [
                        { "price": ".52", "size": "25" },
                        { "price": ".53", "size": "60" },
                        { "price": ".54", "size": "10" }
                      ],
                      "timestamp": "123456789000",
                      "hash": "0x0...."
                    }
                """;

        BookEvent event = JSON.parseObject(json, BookEvent.class);

        // Verify top-level fields
        assertNotNull(event);
        assertEquals("book", event.getEventType());
        assertEquals("65818619657568813474341868652308942079804919287380422192892211131408793125422", event.getAssetId());
        assertEquals("0xbd31dc8a20211944f6b70f31557f1001557b59905b7738480ca09bd4532f84af", event.getMarket());
        assertEquals("123456789000", event.getTimestamp());
        assertEquals("0x0....", event.getHash());
        assertEquals(123456789000L, event.getTimestampAsLong());

        // Verify bids
        List<OrderBookEntry> bids = event.getBids();
        assertNotNull(bids);
        assertEquals(3, bids.size());

        assertEquals(".48", bids.get(0).getPrice());
        assertEquals("30", bids.get(0).getSize());
        assertEquals(".49", bids.get(1).getPrice());
        assertEquals("20", bids.get(1).getSize());
        assertEquals(".50", bids.get(2).getPrice());
        assertEquals("15", bids.get(2).getSize());

        // Verify asks
        List<OrderBookEntry> asks = event.getAsks();
        assertNotNull(asks);
        assertEquals(3, asks.size());

        assertEquals(".52", asks.get(0).getPrice());
        assertEquals("25", asks.get(0).getSize());
        assertEquals(".53", asks.get(1).getPrice());
        assertEquals("60", asks.get(1).getSize());
        assertEquals(".54", asks.get(2).getPrice());
        assertEquals("10", asks.get(2).getSize());

        // Verify best bid and ask helpers
        OrderBookEntry bestBid = event.getBestBid();
        assertNotNull(bestBid);
        assertEquals(".48", bestBid.getPrice());

        OrderBookEntry bestAsk = event.getBestAsk();
        assertNotNull(bestAsk);
        assertEquals(".52", bestAsk.getPrice());
    }

    /**
     * Tests that BookEvent handles empty bids/asks lists.
     */
    @Test
    void testBookEventWithEmptyLists() throws Exception {
        String json = """
                    {
                      "event_type": "book",
                      "asset_id": "test-asset",
                      "market": "test-market",
                      "bids": [],
                      "asks": [],
                      "timestamp": "123456789000",
                      "hash": "0x0"
                    }
                """;

        BookEvent event = JSON.parseObject(json, BookEvent.class);

        assertNotNull(event);
        assertNotNull(event.getBids());
        assertNotNull(event.getAsks());
        assertEquals(0, event.getBids().size());
        assertEquals(0, event.getAsks().size());
        assertNull(event.getBestBid());
        assertNull(event.getBestAsk());
    }

    /**
     * Tests BookEvent builder pattern.
     */
    @Test
    void testBookEventBuilder() {
        OrderBookEntry bid = new OrderBookEntry("0.5", "100");
        OrderBookEntry ask = new OrderBookEntry("0.6", "200");

        BookEvent event = BookEvent.builder()
                .eventType("book")
                .assetId("test-asset")
                .market("test-market")
                .bids(List.of(bid))
                .asks(List.of(ask))
                .timestamp("123456789")
                .hash("0xtest")
                .build();

        assertEquals("book", event.getEventType());
        assertEquals("test-asset", event.getAssetId());
        assertEquals(1, event.getBids().size());
        assertEquals(1, event.getAsks().size());
        assertEquals("0.5", event.getBestBid().getPrice());
        assertEquals("0.6", event.getBestAsk().getPrice());
    }
}

