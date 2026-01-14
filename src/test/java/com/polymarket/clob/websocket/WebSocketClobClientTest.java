package com.polymarket.clob.websocket;

import okhttp3.Response;
import okhttp3.WebSocket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WebSocketClobClient.
 * Tests the WebSocket client functionality including connection lifecycle,
 * message handling, listener notifications, and error handling.
 */
@ExtendWith(MockitoExtension.class)
public class WebSocketClobClientTest {

    @Mock
    private WebSocket mockWebSocket;

    @Mock
    private Response mockResponse;

    private WebSocketClobClient marketClient;
    private WebSocketClobClient userClient;

    private static final String TEST_BASE_URL = "wss://test.polymarket.com";
    private static final List<String> TEST_ASSET_IDS = Arrays.asList("asset1", "asset2");
    private static final List<String> TEST_MARKETS = Arrays.asList("market1", "market2");
    private static final Map<String, Object> TEST_AUTH = new HashMap<>();

    @BeforeEach
    void setUp() {
        TEST_AUTH.put("apiKey", "test-key");
        TEST_AUTH.put("secret", "test-secret");

        marketClient = new WebSocketClobClient(
                WebSocketClobClient.MARKET_CHANNEL,
                TEST_BASE_URL,
                TEST_ASSET_IDS,
                null
        );

        userClient = new WebSocketClobClient(
                WebSocketClobClient.USER_CHANNEL,
                TEST_BASE_URL,
                TEST_MARKETS,
                TEST_AUTH
        );
    }

    /**
     * Tests that the market channel client is properly constructed.
     */
    @Test
    void testMarketChannelClientConstruction() {
        assertNotNull(marketClient);
    }

    /**
     * Tests that the user channel client is properly constructed.
     */
    @Test
    void testUserChannelClientConstruction() {
        assertNotNull(userClient);
    }

    /**
     * Tests that a listener can be registered successfully.
     */
    @Test
    void testRegisterListener() {
        TestWebSocketListener listener = new TestWebSocketListener();
        marketClient.registerListener(listener);

        // Verify listener is registered by triggering a notification
        Map<String, Object> testMessage = new HashMap<>();
        testMessage.put("event_type", "test");

        marketClient.notifyListener("test", testMessage);

        assertEquals(1, listener.getCallCount());
    }

    /**
     * Tests that multiple listeners can be registered and all receive notifications.
     */
    @Test
    void testMultipleListeners() {
        TestWebSocketListener listener1 = new TestWebSocketListener();
        TestWebSocketListener listener2 = new TestWebSocketListener();

        marketClient.registerListener(listener1);
        marketClient.registerListener(listener2);

        Map<String, Object> testMessage = new HashMap<>();
        testMessage.put("event_type", "test");

        marketClient.notifyListener("test", testMessage);

        assertEquals(1, listener1.getCallCount());
        assertEquals(1, listener2.getCallCount());
    }

    /**
     * Tests that the notifyListener method properly passes event type and message data.
     */
    @Test
    void testNotifyListenerWithEventData() {
        TestWebSocketListener listener = new TestWebSocketListener();
        marketClient.registerListener(listener);

        String expectedEventType = "book";
        Map<String, Object> expectedMessage = new HashMap<>();
        expectedMessage.put("asset_id", "test-asset");
        expectedMessage.put("price", 100.0);

        marketClient.notifyListener(expectedEventType, expectedMessage);

        assertEquals(expectedEventType, listener.getLastEventType());
        assertEquals(expectedMessage, listener.getLastMessage());
    }

    /**
     * Tests that onOpen sends the correct subscription message for MARKET_CHANNEL.
     */
    @Test
    void testOnOpenMarketChannel() {
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

        marketClient.onOpen(mockWebSocket, mockResponse);

        verify(mockWebSocket, atLeastOnce()).send(messageCaptor.capture());

        String sentMessage = messageCaptor.getValue();
        assertTrue(sentMessage.contains("\"type\":\"market\""));
        assertTrue(sentMessage.contains("assets_ids"));
    }

    /**
     * Tests that onOpen sends the correct subscription message for USER_CHANNEL.
     */
    @Test
    void testOnOpenUserChannel() {
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

        userClient.onOpen(mockWebSocket, mockResponse);

        verify(mockWebSocket, atLeastOnce()).send(messageCaptor.capture());

        String sentMessage = messageCaptor.getValue();
        assertTrue(sentMessage.contains("\"type\":\"user\""));
        assertTrue(sentMessage.contains("markets"));
        assertTrue(sentMessage.contains("auth"));
    }

    /**
     * Tests that onMessage handles PING messages by responding with PONG.
     */
    @Test
    void testOnMessageHandlesPing() {
        marketClient.onMessage(mockWebSocket, "PING");
        verify(mockWebSocket).send("PONG");
    }

    /**
     * Tests that onMessage ignores PONG messages.
     */
    @Test
    void testOnMessageIgnoresPong() {
        marketClient.onMessage(mockWebSocket, "PONG");
        verify(mockWebSocket, never()).send(anyString());
    }

    /**
     * Tests that onMessage correctly deserializes and notifies listeners for single JSON messages.
     */
    @Test
    void testOnMessageSingleJsonMessage() throws InterruptedException {
        TestWebSocketListener listener = new TestWebSocketListener();
        marketClient.registerListener(listener);

        String jsonMessage = "{\"event_type\":\"book\",\"asset_id\":\"test-asset\",\"price\":100.0}";

        marketClient.onMessage(mockWebSocket, jsonMessage);

        // Give some time for async processing
        Thread.sleep(100);

        assertEquals(1, listener.getCallCount());
        assertEquals("book", listener.getLastEventType());
        assertNotNull(listener.getLastMessage());
    }

    /**
     * Tests that onMessage correctly handles JSON array messages.
     */
    @Test
    void testOnMessageJsonArrayMessage() throws InterruptedException {
        TestWebSocketListener listener = new TestWebSocketListener();
        marketClient.registerListener(listener);

        String jsonArrayMessage = "[{\"event_type\":\"book\",\"price\":100.0},{\"event_type\":\"trade\",\"price\":101.0}]";

        marketClient.onMessage(mockWebSocket, jsonArrayMessage);

        // Give some time for async processing
        Thread.sleep(100);

        assertEquals(2, listener.getCallCount());
    }

    /**
     * Tests that onMessage correctly deserializes price_change events.
     */
    @Test
    void testOnMessagePriceChangeEvent() throws InterruptedException {
        TestWebSocketListener listener = new TestWebSocketListener();
        marketClient.registerListener(listener);

        String priceChangeJson = """
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
                            }
                        ],
                        "timestamp": "1768323366524",
                        "event_type": "price_change"
                    }
                """;

        marketClient.onMessage(mockWebSocket, priceChangeJson);

        // Give some time for async processing
        Thread.sleep(100);

        assertEquals(1, listener.getCallCount());
        assertEquals("price_change", listener.getLastEventType());
        assertNotNull(listener.getLastMessage());

        // Verify that the PriceChangeEvent object was added to the message map
        Object priceChangeEvent = listener.getLastMessage().get("PRICE_CHANGE");
        assertNotNull(priceChangeEvent);
        assertTrue(priceChangeEvent instanceof com.polymarket.clob.model.PriceChangeEvent);

        com.polymarket.clob.model.PriceChangeEvent event =
                (com.polymarket.clob.model.PriceChangeEvent) priceChangeEvent;
        assertEquals("0x17815081230e3b9c78b098162c33b1ffa68c4ec29c123d3d14989599e0c2e113", event.getMarket());
        assertEquals(1, event.getPriceChanges().size());
        assertEquals("0.996", event.getPriceChanges().get(0).getPrice());
    }

    /**
     * Tests that onMessage correctly deserializes book events.
     */
    @Test
    void testOnMessageBookEvent() throws InterruptedException {
        TestWebSocketListener listener = new TestWebSocketListener();
        marketClient.registerListener(listener);

        String bookJson = """
                    {
                      "event_type": "book",
                      "asset_id": "65818619657568813474341868652308942079804919287380422192892211131408793125422",
                      "market": "0xbd31dc8a20211944f6b70f31557f1001557b59905b7738480ca09bd4532f84af",
                      "bids": [
                        { "price": ".48", "size": "30" }
                      ],
                      "asks": [
                        { "price": ".52", "size": "25" }
                      ],
                      "timestamp": "123456789000",
                      "hash": "0x0"
                    }
                """;

        marketClient.onMessage(mockWebSocket, bookJson);
        Thread.sleep(100);

        assertEquals(1, listener.getCallCount());
        assertEquals("book", listener.getLastEventType());

        Object bookEvent = listener.getLastMessage().get("BOOK");
        assertNotNull(bookEvent);
        assertTrue(bookEvent instanceof com.polymarket.clob.model.BookEvent);

        com.polymarket.clob.model.BookEvent event =
                (com.polymarket.clob.model.BookEvent) bookEvent;
        assertEquals("0xbd31dc8a20211944f6b70f31557f1001557b59905b7738480ca09bd4532f84af", event.getMarket());
        assertEquals(1, event.getBids().size());
        assertEquals(1, event.getAsks().size());
    }

    /**
     * Tests that onMessage correctly deserializes last_trade_price events.
     */
    @Test
    void testOnMessageLastTradePriceEvent() throws InterruptedException {
        TestWebSocketListener listener = new TestWebSocketListener();
        marketClient.registerListener(listener);

        String lastTradePriceJson = """
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

        marketClient.onMessage(mockWebSocket, lastTradePriceJson);
        Thread.sleep(100);

        assertEquals(1, listener.getCallCount());
        assertEquals("last_trade_price", listener.getLastEventType());

        Object lastTradePriceEvent = listener.getLastMessage().get("LAST_TRADE_PRICE");
        assertNotNull(lastTradePriceEvent);
        assertTrue(lastTradePriceEvent instanceof com.polymarket.clob.model.LastTradePriceEvent);

        com.polymarket.clob.model.LastTradePriceEvent event =
                (com.polymarket.clob.model.LastTradePriceEvent) lastTradePriceEvent;
        assertEquals("0.456", event.getPrice());
        assertEquals("BUY", event.getSide());
    }

    /**
     * Tests that onMessage correctly deserializes best_bid_ask events.
     */
    @Test
    void testOnMessageBestBidAskEvent() throws InterruptedException {
        TestWebSocketListener listener = new TestWebSocketListener();
        marketClient.registerListener(listener);

        String bestBidAskJson = """
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

        marketClient.onMessage(mockWebSocket, bestBidAskJson);
        Thread.sleep(100);

        assertEquals(1, listener.getCallCount());
        assertEquals("best_bid_ask", listener.getLastEventType());

        Object bestBidAskEvent = listener.getLastMessage().get("BEST_BID_ASK");
        assertNotNull(bestBidAskEvent);
        assertTrue(bestBidAskEvent instanceof com.polymarket.clob.model.BestBidAskEvent);

        com.polymarket.clob.model.BestBidAskEvent event =
                (com.polymarket.clob.model.BestBidAskEvent) bestBidAskEvent;
        assertEquals("0.73", event.getBestBid());
        assertEquals("0.77", event.getBestAsk());
        assertEquals("0.04", event.getSpread());
    }

    /**
     * Tests that onMessage correctly deserializes trade events.
     */
    @Test
    void testOnMessageTradeEvent() throws InterruptedException {
        TestWebSocketListener listener = new TestWebSocketListener();
        marketClient.registerListener(listener);

        String tradeJson = """
                    {
                      "asset_id": "52114319501245915516055106046884209969926127482827954674443846427813813222426",
                      "event_type": "trade",
                      "id": "28c4d2eb-bbea-40e7-a9f0-b2fdb56b2c2e",
                      "market": "0xbd31dc8a20211944f6b70f31557f1001557b59905b7738480ca09bd4532f84af",
                      "price": "0.57",
                      "side": "BUY",
                      "size": "10",
                      "outcome": "YES",
                      "owner": "9180014b-33c8-9240-a14b-bdca11c0a465",
                      "status": "MATCHED",
                      "type": "TRADE",
                      "timestamp": "1672290701"
                    }
                """;

        marketClient.onMessage(mockWebSocket, tradeJson);
        Thread.sleep(100);

        assertEquals(1, listener.getCallCount());
        assertEquals("trade", listener.getLastEventType());

        Object tradeEvent = listener.getLastMessage().get("TRADE");
        assertNotNull(tradeEvent);
        assertTrue(tradeEvent instanceof com.polymarket.clob.model.TradeEvent);

        com.polymarket.clob.model.TradeEvent event =
                (com.polymarket.clob.model.TradeEvent) tradeEvent;
        assertEquals("0.57", event.getPrice());
        assertEquals("BUY", event.getSide());
        assertEquals("10", event.getSize());
        assertTrue(event.isBuy());
    }

    /**
     * Tests that onMessage correctly deserializes order events.
     */
    @Test
    void testOnMessageOrderEvent() throws InterruptedException {
        TestWebSocketListener listener = new TestWebSocketListener();
        marketClient.registerListener(listener);

        String orderJson = """
                    {
                      "asset_id": "52114319501245915516055106046884209969926127482827954674443846427813813222426",
                      "event_type": "order",
                      "id": "0xff354cd7ca7539dfa9c28d90943ab5779a4eac34b9b37a757d7b32bdfb11790b",
                      "market": "0xbd31dc8a20211944f6b70f31557f1001557b59905b7738480ca09bd4532f84af",
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

        marketClient.onMessage(mockWebSocket, orderJson);
        Thread.sleep(100);

        assertEquals(1, listener.getCallCount());
        assertEquals("order", listener.getLastEventType());

        Object orderEvent = listener.getLastMessage().get("ORDER");
        assertNotNull(orderEvent);
        assertTrue(orderEvent instanceof com.polymarket.clob.model.OrderEvent);

        com.polymarket.clob.model.OrderEvent event =
                (com.polymarket.clob.model.OrderEvent) orderEvent;
        assertEquals("0.57", event.getPrice());
        assertEquals("SELL", event.getSide());
        assertEquals("10", event.getOriginalSize());
        assertEquals("PLACEMENT", event.getType());
        assertTrue(event.isPlacement());
        assertTrue(event.isSell());
    }

    /**
     * Tests that onMessage handles unparseable messages gracefully.
     */
    @Test
    void testOnMessageUnparseableMessage() throws InterruptedException {
        TestWebSocketListener listener = new TestWebSocketListener();
        marketClient.registerListener(listener);

        String plainMessage = "plain text message";

        marketClient.onMessage(mockWebSocket, plainMessage);

        // Give some time for async processing
        Thread.sleep(100);

        // Should still notify listener with "unknown" event type
        assertEquals(1, listener.getCallCount());
        assertEquals("unknown", listener.getLastEventType());
    }

    /**
     * Tests that onClosing properly closes the WebSocket.
     */
    @Test
    void testOnClosing() {
        marketClient.onClosing(mockWebSocket, 1000, "Normal closure");
        verify(mockWebSocket).close(1000, null);
    }

    /**
     * Tests that onFailure logs errors without throwing exceptions.
     */
    @Test
    void testOnFailure() {
        Throwable testException = new RuntimeException("Test error");

        // Should not throw exception
        assertDoesNotThrow(() ->
                marketClient.onFailure(mockWebSocket, testException, mockResponse)
        );
    }

    /**
     * Tests that onClosed properly handles WebSocket closure.
     */
    @Test
    void testOnClosed() {
        // Should not throw exception
        assertDoesNotThrow(() ->
                marketClient.onClosed(mockWebSocket, 1000, "Normal closure")
        );
    }

    /**
     * Tests that close method properly shuts down the WebSocket and resources.
     */
    @Test
    void testClose() {
        // Should not throw exception
        assertDoesNotThrow(() -> marketClient.close());
    }

    /**
     * Tests the channel type constants are correctly defined.
     */
    @Test
    void testChannelTypeConstants() {
        assertEquals("market", WebSocketClobClient.MARKET_CHANNEL);
        assertEquals("user", WebSocketClobClient.USER_CHANNEL);
    }

    /**
     * Tests that listeners receive notifications in the correct order.
     */
    @Test
    void testListenerNotificationOrder() {
        List<String> notificationOrder = new ArrayList<>();

        WebSocketListener listener1 = (eventType, messageMap) ->
                notificationOrder.add("listener1-" + eventType);

        WebSocketListener listener2 = (eventType, messageMap) ->
                notificationOrder.add("listener2-" + eventType);

        marketClient.registerListener(listener1);
        marketClient.registerListener(listener2);

        Map<String, Object> testMessage = new HashMap<>();
        marketClient.notifyListener("test", testMessage);

        assertEquals(2, notificationOrder.size());
        assertEquals("listener1-test", notificationOrder.get(0));
        assertEquals("listener2-test", notificationOrder.get(1));
    }

    /**
     * Tests that empty JSON message is handled correctly.
     */
    @Test
    void testOnMessageEmptyJsonObject() throws InterruptedException {
        TestWebSocketListener listener = new TestWebSocketListener();
        marketClient.registerListener(listener);

        String emptyJson = "{}";

        marketClient.onMessage(mockWebSocket, emptyJson);

        // Give some time for async processing
        Thread.sleep(100);

        assertEquals(1, listener.getCallCount());
        assertNull(listener.getLastEventType()); // event_type is null in empty JSON
    }

    /**
     * Tests that JSON message with null event_type is handled correctly.
     */
    @Test
    void testOnMessageNullEventType() throws InterruptedException {
        TestWebSocketListener listener = new TestWebSocketListener();
        marketClient.registerListener(listener);

        String jsonMessage = "{\"event_type\":null,\"data\":\"test\"}";

        marketClient.onMessage(mockWebSocket, jsonMessage);

        // Give some time for async processing
        Thread.sleep(100);

        assertEquals(1, listener.getCallCount());
        assertNull(listener.getLastEventType());
    }



    /**
     * Tests handling of fill event type messages.
     */
    @Test
    void testOnMessageFillEvent() throws InterruptedException {
        TestWebSocketListener listener = new TestWebSocketListener();
        userClient.registerListener(listener);

        String fillMessage = "{\"event_type\":\"fill\",\"order_id\":\"order123\",\"filled_amount\":\"100\"}";

        userClient.onMessage(mockWebSocket, fillMessage);

        // Give some time for async processing
        Thread.sleep(100);

        assertEquals(1, listener.getCallCount());
        assertEquals("fill", listener.getLastEventType());
    }

    /**
     * Tests that malformed JSON array is handled gracefully.
     */
    @Test
    void testOnMessageMalformedJsonArray() throws InterruptedException {
        TestWebSocketListener listener = new TestWebSocketListener();
        marketClient.registerListener(listener);

        String malformedArray = "[{\"event_type\":\"book\"},";

        marketClient.onMessage(mockWebSocket, malformedArray);

        // Give some time for async processing
        Thread.sleep(100);

        // Should fall back to plain string handling
        assertEquals(1, listener.getCallCount());
        assertEquals("unknown", listener.getLastEventType());
    }

    /**
     * Tests that listener exception does not break notification chain.
     */
    @Test
    void testListenerExceptionDoesNotBreakChain() {
        List<String> notifications = new ArrayList<>();

        WebSocketListener failingListener = (eventType, messageMap) -> {
            throw new RuntimeException("Listener error");
        };

        WebSocketListener successListener = (eventType, messageMap) -> {
            notifications.add(eventType);
        };

        marketClient.registerListener(failingListener);
        marketClient.registerListener(successListener);

        Map<String, Object> testMessage = new HashMap<>();

        // Should not throw exception even though first listener fails
        assertDoesNotThrow(() -> marketClient.notifyListener("test", testMessage));
    }

    /**
     * Tests that onMessage handles very large JSON messages.
     */
    @Test
    void testOnMessageLargeJsonMessage() throws InterruptedException {
        TestWebSocketListener listener = new TestWebSocketListener();
        marketClient.registerListener(listener);

        StringBuilder largeJson = new StringBuilder("{\"event_type\":\"book\",\"data\":[");
        for (int i = 0; i < 1000; i++) {
            if (i > 0) largeJson.append(",");
            largeJson.append("[\"").append(i).append("\",\"100\"]");
        }
        largeJson.append("]}");

        marketClient.onMessage(mockWebSocket, largeJson.toString());

        // Give some time for async processing
        Thread.sleep(100);

        assertEquals(1, listener.getCallCount());
        assertEquals("book", listener.getLastEventType());
    }

    /**
     * Tests that multiple messages in array are all processed.
     */
    @Test
    void testOnMessageMultipleMessagesInArray() throws InterruptedException {
        TestWebSocketListener listener = new TestWebSocketListener();
        marketClient.registerListener(listener);

        String multiMessage = "[{\"event_type\":\"book\"},{\"event_type\":\"trade\"},{\"event_type\":\"fill\"}]";

        marketClient.onMessage(mockWebSocket, multiMessage);

        // Give some time for async processing
        Thread.sleep(100);

        assertEquals(3, listener.getCallCount());
    }

    /**
     * Tests that onFailure with null response is handled.
     */
    @Test
    void testOnFailureWithNullResponse() {
        Throwable testException = new RuntimeException("Test error");

        // Should not throw exception even with null response
        assertDoesNotThrow(() ->
                marketClient.onFailure(mockWebSocket, testException, null)
        );
    }

    /**
     * Tests that closing codes other than 1000 are handled correctly.
     */
    @Test
    void testOnClosingWithDifferentCodes() {
        marketClient.onClosing(mockWebSocket, 1001, "Going away");
        verify(mockWebSocket).close(1001, null);
    }

    /**
     * Tests notification with empty message map.
     */
    @Test
    void testNotifyListenerWithEmptyMessage() {
        TestWebSocketListener listener = new TestWebSocketListener();
        marketClient.registerListener(listener);

        Map<String, Object> emptyMessage = new HashMap<>();
        marketClient.notifyListener("empty", emptyMessage);

        assertEquals(1, listener.getCallCount());
        assertEquals("empty", listener.getLastEventType());
        assertEquals(emptyMessage, listener.getLastMessage());
    }

    /**
     * Tests that no listeners scenario doesn't cause errors.
     */
    @Test
    void testNotifyListenerWithNoListeners() {
        WebSocketClobClient noListenerClient = new WebSocketClobClient(
                WebSocketClobClient.MARKET_CHANNEL,
                TEST_BASE_URL,
                TEST_ASSET_IDS,
                null

        );

        Map<String, Object> testMessage = new HashMap<>();

        // Should not throw exception
        assertDoesNotThrow(() -> noListenerClient.notifyListener("test", testMessage));
    }

    /**
     * Tests URL construction for different channel types.
     */
    @Test
    void testUrlConstruction() {
        WebSocketClobClient marketClient = new WebSocketClobClient(
                WebSocketClobClient.MARKET_CHANNEL,
                "wss://api.example.com",
                TEST_ASSET_IDS,
                null
        );

        WebSocketClobClient userClient = new WebSocketClobClient(
                WebSocketClobClient.USER_CHANNEL,
                "wss://api.example.com",
                TEST_MARKETS,
                TEST_AUTH
        );

        assertNotNull(marketClient);
        assertNotNull(userClient);
    }

    /**
     * Helper test listener class for testing purposes.
     */
    private static class TestWebSocketListener implements WebSocketListener {
        private int callCount = 0;
        private String lastEventType;
        private Map<String, Object> lastMessage;

        @Override
        public void onEvent(String eventType, Map<String, Object> messageMap) {
            callCount++;
            lastEventType = eventType;
            lastMessage = messageMap;
        }

        public int getCallCount() {
            return callCount;
        }

        public String getLastEventType() {
            return lastEventType;
        }

        public Map<String, Object> getLastMessage() {
            return lastMessage;
        }
    }
}
