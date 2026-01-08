package com.polymarket.clob.websocket;

import java.util.Map;

/**
 * Listener interface for receiving WebSocket event notifications from {@link WebSocketClobClient}.
 * Implementations of this interface can be registered with a WebSocketClobClient to receive
 * real-time updates about market data or user-specific events.
 *
 * <p>Common event types include:
 * <ul>
 *   <li>"book" - Order book updates</li>
 *   <li>"trade" - Trade executions</li>
 *   <li>"fill" - User order fills</li>
 *   <li>"unknown" - Unrecognized messages</li>
 * </ul>
 *
 * @see WebSocketClobClient#registerListener(WebSocketListener)
 */
public interface WebSocketListener {

    /**
     * Called when a WebSocket event is received.
     * Implementations should handle the event based on the eventType and process the messageMap accordingly.
     *
     * @param eventType  the type of event received (e.g., "book", "trade", "fill")
     * @param messageMap the event data as a map containing various fields depending on the event type
     */
    void notify(String eventType, Map<String, Object> messageMap);
}
