package com.polymarket.clob.websocket;


import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * WebSocket client for Polymarket order book data.
 * This client connects to the Polymarket WebSocket API and handles real-time market and user data streams.
 *
 * <p>Supports two channel types:
 * <ul>
 *   <li>{@link #MARKET_CHANNEL} - Market data (order book, trades) for specific asset IDs</li>
 *   <li>{@link #USER_CHANNEL} - User-specific data (orders, fills) for specific markets</li>
 * </ul>
 *
 * <p>The client automatically handles:
 * <ul>
 *   <li>WebSocket connection lifecycle</li>
 *   <li>Ping/Pong messages to keep connection alive</li>
 *   <li>Message deserialization and event distribution</li>
 *   <li>Listener notification pattern</li>
 * </ul>
 *
 * @see WebSocketListener
 */
public class WebSocketClobClient extends okhttp3.WebSocketListener {
    private static final Logger logger = LogManager.getLogger(WebSocketClobClient.class);

    /**
     * Channel type constant for market data streams (order book, trades)
     */
    public static final String MARKET_CHANNEL = "market";

    /**
     * Channel type constant for user-specific data streams (orders, fills)
     */
    public static final String USER_CHANNEL = "user";


    private final String channelType;
    private final String url;
    private final List<String> data; // asset_ids or markets
    private final Map<String, Object> auth;
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler;

    private WebSocket webSocket;

    private List<WebSocketListener> listeners = new ArrayList<>();


    /**
     * Constructs a new WebSocketClobClient.
     *
     * @param channelType the channel type - either {@link #MARKET_CHANNEL} or {@link #USER_CHANNEL}
     * @param baseUrl     the base URL of the WebSocket endpoint (without the /ws path)
     * @param data        list of asset IDs (for MARKET_CHANNEL) or markets (for USER_CHANNEL) to subscribe to
     * @param auth        authentication map containing credentials (required for USER_CHANNEL, can be null for MARKET_CHANNEL)
     */
    public WebSocketClobClient(String channelType, String baseUrl, List<String> data, Map<String, Object> auth) {
        this.channelType = channelType;
        this.url = baseUrl + "/ws/" + channelType;
        this.data = data;
        this.auth = auth;
        this.objectMapper = new ObjectMapper();
        this.client = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS) // No timeout for WebSocket
                .build();
        this.scheduler = Executors.newScheduledThreadPool(1);//threads to allocate for the scheduler (used for ping/pong mechanism)
    }


    /**
     * Registers a listener to receive WebSocket event notifications.
     * Multiple listeners can be registered and will all be notified when events occur.
     *
     * @param listener the listener to register for event notifications
     * @see WebSocketListener#onEvent(String, Map)
     */
    public void registerListener(WebSocketListener listener) {
        this.listeners.add(listener);
    }


    /**
     * Notifies all registered listeners about a WebSocket event.
     * This method iterates through all registered listeners and calls their notify method.
     * If a listener throws an exception, it is logged and the notification continues to other listeners.
     *
     * @param eventType  the type of the event (e.g., "book", "trade", "fill")
     * @param messageMap the message data as a map of key-value pairs
     * @see WebSocketListener#onEvent(String, Map)
     */
    public void notifyListener(String eventType, Map<String, Object> messageMap) {
        for (WebSocketListener listener : listeners) {
            try {
                listener.onEvent(eventType, messageMap);
            } catch (Exception e) {
                logger.error("Error notifying listener", e);
            }
        }
    }

    /**
     * Initiates the WebSocket connection to the server.
     * Creates a WebSocket request and establishes the connection.
     * Upon successful connection, the {@link #onOpen(WebSocket, Response)} callback will be invoked.
     */
    public void run() {
        Request request = new Request.Builder()
                .url(url)
                .build();

        webSocket = client.newWebSocket(request, this);
    }

    /**
     * Callback invoked when the WebSocket connection is successfully established.
     * Sends the initial subscription message for the configured channel type and starts the ping scheduler.
     *
     * <p>For MARKET_CHANNEL, subscribes to asset IDs for market data.
     * For USER_CHANNEL, subscribes to markets with authentication credentials.
     *
     * @param webSocket the WebSocket instance that was opened
     * @param response  the HTTP response from the server that initiated the WebSocket handshake
     */
    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        logger.info("WebSocket connected to {}", url);

        try {
            // Send subscription message
            Map<String, Object> subscriptionMsg = new HashMap<>();
            subscriptionMsg.put("type", channelType);

            if (MARKET_CHANNEL.equals(channelType)) {
                subscriptionMsg.put("assets_ids", data);
            } else if (USER_CHANNEL.equals(channelType)) {
                subscriptionMsg.put("markets", data);
                subscriptionMsg.put("auth", auth);
            }

            String jsonMsg = objectMapper.writeValueAsString(subscriptionMsg);
            webSocket.send(jsonMsg);
            logger.info("Sent subscription message: {}", jsonMsg);

            // Start ping thread
            startPingScheduler(webSocket);

        } catch (Exception e) {
            logger.error("Error in onOpen", e);
        }
    }

    /**
     * Callback invoked when a text message is received from the WebSocket.
     *
     * <p>Handles three types of messages:
     * <ul>
     *   <li>Simple protocol messages: "PING" (responds with "PONG"), "PONG" (ignored)</li>
     *   <li>JSON messages: Deserializes and notifies listeners with event type</li>
     *   <li>JSON arrays: Processes each message in the array</li>
     * </ul>
     *
     * @param webSocket the WebSocket instance that received the message
     * @param text      the text message received
     */
    @Override
    public void onMessage(WebSocket webSocket, String text) {
        try {
            // Handle simple string messages
            if (text.equals("PONG")) {
                return;
            }

            if (text.equals("PING")) {
                webSocket.send("PONG");
                return;
            }

            // Try to deserialize the message
            List<Map<String, Object>> messageList = deserializeMessage(text);

            // Process each message in the list
            for (Map<String, Object> messageMap : messageList) {
                String eventType = (String) messageMap.get("event_type");
                notifyListener(eventType, messageMap);
            }

        } catch (Exception e) {
            logger.error("Error parsing onMessage: {}", text, e);
        }
    }


    /**
     * Deserialize the message text into a list of maps.
     *
     * <p>Handles three cases:
     * <ol>
     *   <li>Map&lt;String, Object&gt; (single message) - wrapped in a list</li>
     *   <li>List&lt;Map&lt;String, Object&gt;&gt; (array of messages) - returned as is</li>
     *   <li>Plain String (unparseable JSON) - wrapped in a map with "raw_message" and "unknown" event_type</li>
     * </ol>
     *
     * @param text the raw message text to deserialize
     * @return a list of message maps, never null
     * @throws Exception if deserialization fails completely (should not happen due to fallback handling)
     */
    private List<Map<String, Object>> deserializeMessage(String text) throws Exception {
        List<Map<String, Object>> messageList = new ArrayList<>();

        try {
            // Try parsing as Map<String, Object> first
            Map<String, Object> singleMessage = objectMapper.readValue(text, Map.class);
            messageList.add(singleMessage);
        } catch (Exception e) {
            try {
                // Try parsing as List<Map<String, Object>>
                messageList = objectMapper.readValue(text,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
            } catch (Exception e2) {
                // If both fail, treat as plain string - wrap it in a map
                logger.warn("Received plain string message: {}", text);
                Map<String, Object> wrapper = new HashMap<>();
                wrapper.put("raw_message", text);
                wrapper.put("event_type", "unknown");
                messageList.add(wrapper);
            }
        }

        return messageList;
    }

    /**
     * Callback invoked when the remote peer has requested to close the WebSocket connection.
     * Closes the WebSocket connection gracefully and shuts down the scheduler.
     *
     * @param webSocket the WebSocket instance being closed
     * @param code      the status code indicating the reason for closure
     * @param reason    a human-readable explanation for the closure (may be empty)
     */
    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        logger.info("WebSocket closing: code={}, reason={}", code, reason);
        webSocket.close(code, null);
        scheduler.shutdown();
    }

    /**
     * Callback invoked when a WebSocket error occurs.
     * Logs the error and shuts down the scheduler.
     *
     * @param webSocket the WebSocket instance that encountered the error
     * @param t         the throwable that caused the failure
     * @param response  the HTTP response (may be null if the failure occurred before the handshake)
     */
    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        logger.error("WebSocket error", t);
        if (response != null) {
            logger.error("Response: {}", response);
        }
        scheduler.shutdown();
    }

    /**
     * Callback invoked when the WebSocket connection has been closed.
     * Logs the closure and shuts down the scheduler.
     *
     * @param webSocket the WebSocket instance that was closed
     * @param code      the status code indicating the reason for closure
     * @param reason    a human-readable explanation for the closure (may be empty)
     */
    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
        logger.info("WebSocket closed: code={}, reason={}", code, reason);
        scheduler.shutdown();
    }

    /**
     * Starts a scheduled task that sends PING messages every 10 seconds to keep the connection alive.
     * The server is expected to respond with PONG messages.
     *
     * @param webSocket the WebSocket instance to send PING messages on
     */
    private void startPingScheduler(WebSocket webSocket) {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                webSocket.send("PING");
            } catch (Exception e) {
                logger.error("Error sending PING", e);
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    /**
     * Closes the WebSocket connection gracefully and cleans up resources.
     * This method closes the WebSocket with a normal closure status code (1000),
     * shuts down the scheduler, dispatcher, and connection pool.
     */
    public void close() {
        if (webSocket != null) {
            webSocket.close(1000, "Client closing");
        }
        scheduler.shutdown();
        client.dispatcher().executorService().shutdown();
        client.connectionPool().evictAll();
    }
}
