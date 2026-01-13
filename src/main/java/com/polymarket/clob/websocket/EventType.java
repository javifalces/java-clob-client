package com.polymarket.clob.websocket;

/**
 * Enumeration of WebSocket event types received from the Polymarket CLOB.
 * These events can be received on either MARKET or USER channels depending on the event type.
 */
public enum EventType {

    /**
     * Order book updates - full snapshots of bids and asks
     * Channel: MARKET
     */
    BOOK("book"),

    /**
     * Price change events with best bid/ask updates
     * Channel: MARKET
     */
    PRICE_CHANGE("price_change"),

    /**
     * Last trade price updates
     * Channel: MARKET
     */
    LAST_TRADE_PRICE("last_trade_price"),

    /**
     * Best bid and ask price updates with spread
     * Channel: MARKET
     */
    BEST_BID_ASK("best_bid_ask"),

    /**
     * Trade execution events with maker orders
     * Channel: USER
     */
    TRADE("trade"),

    /**
     * Order lifecycle events (placements, matches, cancellations)
     * Channel: USER
     */
    ORDER("order"),

    /**
     * User order fill events
     * Channel: USER
     */
    FILL("fill"),

    /**
     * Unknown or unrecognized event type
     */
    UNKNOWN("unknown");

    private final String value;

    EventType(String value) {
        this.value = value;
    }

    /**
     * Get the string value of the event type as it appears in JSON messages
     *
     * @return the event type string
     */
    public String getValue() {
        return value;
    }

    /**
     * Convert a string value to an EventType enum
     *
     * @param value the string value from the JSON message
     * @return the corresponding EventType, or UNKNOWN if not recognized
     */
    public static EventType fromValue(String value) {
        if (value == null) {
            return UNKNOWN;
        }

        for (EventType type : EventType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }

        return UNKNOWN;
    }

    @Override
    public String toString() {
        return value;
    }
}

