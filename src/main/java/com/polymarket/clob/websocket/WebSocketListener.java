package com.polymarket.clob.websocket;

import java.util.Map;

/**
 * Listener interface for receiving WebSocket event notifications from {@link WebSocketClobClient}.
 * Implementations of this interface can be registered with a WebSocketClobClient to receive
 * real-time updates about market data or user-specific events.
 *
 * <p>Common event types include:
 * <ul>
 *   <li>"book" - Order book updates (contains a deserialized BookEvent object in the map under key "book_event")</li>
 *   <li>"trade" - Trade executions (contains a deserialized TradeEvent object in the map under key "trade_event")</li>
 *   <li>"order" - Order events/placements (contains a deserialized OrderEvent object in the map under key "order_event")</li>
 *   <li>"fill" - User order fills</li>
 *   <li>"price_change" - Price change events (contains a deserialized PriceChangeEvent object in the map under key "price_change_event")</li>
 *   <li>"last_trade_price" - Last trade price events (contains a deserialized LastTradePriceEvent object in the map under key "last_trade_price_event")</li>
 *   <li>"best_bid_ask" - Best bid/ask updates (contains a deserialized BestBidAskEvent object in the map under key "best_bid_ask_event")</li>
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
    void onEvent(String eventType, Map<String, Object> messageMap);
}
