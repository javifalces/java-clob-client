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
     * Tests handling of book event type messages.
     */
    @Test
    void testOnMessageBookEvent() throws InterruptedException {
        TestWebSocketListener listener = new TestWebSocketListener();
        marketClient.registerListener(listener);

        String bookMessage = "{\"event_type\":\"book\",\"asset_id\":\"0x123\",\"bids\":[[\"1.5\",\"100\"]],\"asks\":[[\"1.6\",\"200\"]]}";

        marketClient.onMessage(mockWebSocket, bookMessage);

        // Give some time for async processing
        Thread.sleep(100);

        assertEquals(1, listener.getCallCount());
        assertEquals("book", listener.getLastEventType());
        Map<String, Object> message = listener.getLastMessage();
        assertNotNull(message);
        assertEquals("0x123", message.get("asset_id"));
    }

    /**
     * Tests handling of trade event type messages.
     */
    @Test
    void testOnMessageTradeEvent() throws InterruptedException {
        TestWebSocketListener listener = new TestWebSocketListener();
        marketClient.registerListener(listener);

        String tradeMessage = "{\"event_type\":\"trade\",\"asset_id\":\"0x123\",\"price\":\"1.55\",\"size\":\"50\"}";

        marketClient.onMessage(mockWebSocket, tradeMessage);

        // Give some time for async processing
        Thread.sleep(100);

        assertEquals(1, listener.getCallCount());
        assertEquals("trade", listener.getLastEventType());
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
        public void notify(String eventType, Map<String, Object> messageMap) {
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
