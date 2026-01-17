package com.polymarket.clob.websocket;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import okhttp3.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.*;

import static com.polymarket.clob.websocket.EventType.*;

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
    private final ScheduledExecutorService scheduler;

    private WebSocket webSocket;

    private List<WebSocketListener> listeners = new ArrayList<>();

    private volatile boolean isClosedByUser = false;
    private volatile boolean isReconnecting = false;
    private final int maxReconnectAttempts = 5;
    private final long reconnectDelayMs = 3000;
    private int reconnectAttempts = 0;
    private ScheduledFuture<?> pingTask = null;


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
        this.scheduler = Executors.newScheduledThreadPool(1);//threads to allocate for the scheduler (used for ping/pong mechanism)

        this.client = new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES)) // Connection pooling
//                .pingInterval(3, TimeUnit.SECONDS)         // Built-in ping/pong mechanism
                .readTimeout(0, TimeUnit.MILLISECONDS) // No timeout for WebSocket
                .build();

//        this.client = new OkHttpClient.Builder()
//                .readTimeout(0, TimeUnit.MILLISECONDS)
//                .connectTimeout(5, TimeUnit.SECONDS)        // Faster connection timeout
//                .pingInterval(10, TimeUnit.SECONDS)         // Built-in ping/pong mechanism
//                .retryOnConnectionFailure(true)             // Auto-retry failed connections
//                .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES)) // Connection pooling
//                .protocols(Arrays.asList(Protocol.HTTP_1_1)) // HTTP/1.1 for WebSocket
//                .build();

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
        isClosedByUser = false;
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
        reconnectAttempts = 0; // Reset reconnection attempts on successful connection
        isReconnecting = false;

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

            String jsonMsg = JSON.toJSONString(subscriptionMsg);
            webSocket.send(jsonMsg);
            logger.info("Sent subscription message: {}", jsonMsg);
            // Start ping scheduler
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

            // Check if message is a JSON array
            String trimmed = text.trim();
            if (trimmed.startsWith("[")) {
                // Handle JSON array - process each message
                try {
                    com.alibaba.fastjson2.JSONArray jsonArray = com.alibaba.fastjson2.JSONArray.parseArray(text);
                    for (int i = 0; i < jsonArray.size(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        processJsonMessage(jsonObject);
                    }
                } catch (Exception e) {
                    logger.error("Error parsing onMessage: {}", text, e);
                    // Fallback: notify as unknown
                    Map<String, Object> messageMap = new HashMap<>();
                    messageMap.put("raw_message", text);
                    notifyListener(EventType.UNKNOWN.getValue(), messageMap);
                }
                return;
            }

            // Try to parse as single JSON object
            try {
                JSONObject jsonObject = JSONObject.parseObject(text);
                processJsonMessage(jsonObject);
            } catch (Exception e) {
                logger.error("Error parsing onMessage: {}", text, e);
                // Fallback: notify as unknown
                Map<String, Object> messageMap = new HashMap<>();
                messageMap.put("raw_message", text);
                notifyListener(EventType.UNKNOWN.getValue(), messageMap);
            }

        } catch (Exception e) {
            logger.error("Error in onMessage: {}", text, e);
        }
    }

    /**
     * Process a single JSON message object
     */
    private void processJsonMessage(JSONObject jsonObject) {
        String eventTypeStr = jsonObject.getString("event_type");
        EventType eventType = EventType.fromValue(eventTypeStr);
        Map<String, Object> messageMap = new HashMap<>();

        // Deserialize specific event types to typed objects
        try {
            logger.debug("Processing {} {} event: {}", url, eventType, jsonObject.toJSONString());

            switch (eventType) {
                case PRICE_CHANGE:
                    com.polymarket.clob.model.PriceChangeEvent priceChangeEvent = jsonObject.to(com.polymarket.clob.model.PriceChangeEvent.class);
                    messageMap.put(eventType.name(), priceChangeEvent);
                    break;

                case BOOK:
                    com.polymarket.clob.model.BookEvent bookEvent = jsonObject.to(com.polymarket.clob.model.BookEvent.class);
                    messageMap.put(eventType.name(), bookEvent);
                    break;

                case LAST_TRADE_PRICE:
                    com.polymarket.clob.model.LastTradePriceEvent lastTradePriceEvent = jsonObject.to(com.polymarket.clob.model.LastTradePriceEvent.class);
                    messageMap.put(eventType.name(), lastTradePriceEvent);
                    break;

                case BEST_BID_ASK:
                    com.polymarket.clob.model.BestBidAskEvent bestBidAskEvent = jsonObject.to(com.polymarket.clob.model.BestBidAskEvent.class);
                    messageMap.put(eventType.name(), bestBidAskEvent);
                    break;

                case TRADE:
                    com.polymarket.clob.model.TradeEvent tradeEvent = jsonObject.to(com.polymarket.clob.model.TradeEvent.class);
                    messageMap.put(eventType.name(), tradeEvent);
                    break;

                case ORDER:
                    com.polymarket.clob.model.OrderEvent orderEvent = jsonObject.to(com.polymarket.clob.model.OrderEvent.class);
                    messageMap.put(eventType.name(), orderEvent);
                    break;

                case FILL:
                case UNKNOWN:
                default:
                    // No special deserialization for these event types
                    break;
            }
        } catch (Exception e) {
            logger.error("Error deserializing {} event", eventType, e);
        }

        notifyListener(eventTypeStr, messageMap);
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
        logger.info("WebSocket closing {}: code={}, reason={}", url, code, reason);
        webSocket.close(code, null);
    }

    /**
     * Callback invoked when a WebSocket error occurs.
     * Logs the error and attempts to reconnect if not closed by user.
     *
     * @param webSocket the WebSocket instance that encountered the error
     * @param t         the throwable that caused the failure
     * @param response  the HTTP response (may be null if the failure occurred before the handshake)
     */
    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        logger.error("WebSocket error {}", url, t);
        if (response != null) {
            logger.error("Response: {}", response);
        }

        // Stop ping scheduler
        stopPingScheduler();

        // Attempt reconnection if not closed by user
        if (!isClosedByUser) {
            attemptReconnect();
        }
    }

    /**
     * Callback invoked when the WebSocket connection has been closed.
     * Logs the closure and attempts to reconnect if not closed by user.
     *
     * @param webSocket the WebSocket instance that was closed
     * @param code      the status code indicating the reason for closure
     * @param reason    a human-readable explanation for the closure (may be empty)
     */
    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
        logger.info("WebSocket closed {}: code={}, reason={}", url, code, reason);

        // Stop ping scheduler
        stopPingScheduler();

        // Attempt reconnection if not closed by user (code 1000 is normal closure)
        if (!isClosedByUser && code != 1000) {
            attemptReconnect();
        }
    }

    private void startPingScheduler(WebSocket webSocket) {
        // Cancel existing ping task if any
        stopPingScheduler();

        pingTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                if (webSocket != null && !isClosedByUser) {
                    webSocket.send("PING");
                }
            } catch (Exception e) {
                logger.error("Error sending PING", e);
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    private void stopPingScheduler() {
        if (pingTask != null && !pingTask.isCancelled()) {
            pingTask.cancel(false);
            pingTask = null;
        }
    }

    /**
     * Closes the WebSocket connection gracefully and cleans up resources.
     * This method closes the WebSocket with a normal closure status code (1000),
     * shuts down the scheduler, dispatcher, and connection pool.
     */
    public void close() {
        isClosedByUser = true; // Mark as user-initiated close to prevent reconnection

        // Stop ping scheduler
        stopPingScheduler();

        if (webSocket != null) {
            webSocket.close(1000, "Client closing");
        }
        scheduler.shutdown();
        client.dispatcher().executorService().shutdown();
        client.connectionPool().evictAll();
    }

    /**
     * Attempts to reconnect to the WebSocket server with exponential backoff.
     * Will retry up to maxReconnectAttempts times before giving up.
     */
    private void attemptReconnect() {
        if (isReconnecting || isClosedByUser) {
            return;
        }

        if (reconnectAttempts >= maxReconnectAttempts) {
            logger.error("Max reconnection attempts {} ({}) reached. Giving up.", url, maxReconnectAttempts);
            return;
        }

        isReconnecting = true;
        reconnectAttempts++;

        long delay = reconnectDelayMs * reconnectAttempts; // Simple linear backoff
        logger.info("Attempting to reconnect {} (attempt {}/{}) in {} ms...",
                url, reconnectAttempts, maxReconnectAttempts, delay);

        // Schedule reconnection attempt using the existing scheduler
        scheduler.schedule(() -> {
            try {
                run(); // Attempt to reconnect
            } catch (Exception e) {
                logger.error("Reconnection attempt failed {}", url, e);
                isReconnecting = false;
                // Try again
                attemptReconnect();
            }
        }, delay, TimeUnit.MILLISECONDS);
    }
}
