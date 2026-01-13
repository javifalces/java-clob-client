package com.polymarket.clob.examples;

import com.polymarket.clob.model.*;
import com.polymarket.clob.websocket.WebSocketClobClient;
import com.polymarket.clob.websocket.WebSocketListener;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Example demonstrating how to listen for trade and order events from the WebSocket stream.
 * These events are typically received on the USER_CHANNEL and require authentication.
 */
public class TradeAndOrderEventsExample {

    public static void main(String[] args) throws InterruptedException {
        // Markets to subscribe to
        List<String> markets = Arrays.asList(
                "0xbd31dc8a20211944f6b70f31557f1001557b59905b7738480ca09bd4532f84af"
        );

        // Authentication credentials (required for user channel)
        Map<String, Object> auth = Map.of(
                "apiKey", "your-api-key",
                "secret", "your-secret"
        );

        // Create WebSocket client for user channel
        String baseUrl = "wss://ws-subscriptions-clob.polymarket.com";
        WebSocketClobClient client = new WebSocketClobClient(
                WebSocketClobClient.USER_CHANNEL,
                baseUrl,
                markets,
                auth
        );

        // Register a listener to handle trade and order events
        client.registerListener(new WebSocketListener() {
            @Override
            public void onEvent(String eventType, Map<String, Object> messageMap) {
                switch (eventType) {
                    case "trade":
                        handleTradeEvent(messageMap);
                        break;

                    case "order":
                        handleOrderEvent(messageMap);
                        break;

                    default:
                        System.out.println("Received event: " + eventType);
                        break;
                }
            }
        });

        // Connect and start listening
        client.run();

        // Keep the program running (use Ctrl+C to stop)
        Thread.sleep(Long.MAX_VALUE);
    }

    /**
     * Handle trade events - completed trade executions
     */
    private static void handleTradeEvent(Map<String, Object> messageMap) {
        TradeEvent event = (TradeEvent) messageMap.get("trade_event");
        if (event == null) return;

        System.out.println("\n=== Trade Event ===");
        System.out.println("Trade ID: " + event.getId());
        System.out.println("Asset: " + event.getAssetId());
        System.out.println("Market: " + event.getMarket());
        System.out.printf("Trade: %s @ %s (%s)%n",
                event.getSize(), event.getPrice(), event.getSide());
        System.out.println("Outcome: " + event.getOutcome());
        System.out.println("Status: " + event.getStatus());
        System.out.println("Owner: " + event.getOwner());
        System.out.println("Taker Order ID: " + event.getTakerOrderId());

        // Calculate trade value
        double tradeValue = event.getTradeValue();
        System.out.printf("Trade Value: %.4f%n", tradeValue);

        // Display maker orders
        if (event.getMakerOrders() != null && !event.getMakerOrders().isEmpty()) {
            System.out.println("\nMaker Orders (" + event.getMakerOrders().size() + "):");
            for (MakerOrder makerOrder : event.getMakerOrders()) {
                System.out.printf("  Order ID: %s%n", makerOrder.getOrderId());
                System.out.printf("  Matched: %s @ %s%n",
                        makerOrder.getMatchedAmount(), makerOrder.getPrice());
                System.out.printf("  Owner: %s%n", makerOrder.getOwner());
                System.out.printf("  Outcome: %s%n", makerOrder.getOutcome());
            }
        }

        // Check if buy or sell
        if (event.isBuy()) {
            System.out.println("Direction: BUY");
        } else if (event.isSell()) {
            System.out.println("Direction: SELL");
        }

        System.out.println("Timestamp: " + event.getTimestamp());
        System.out.println("==================\n");
    }

    /**
     * Handle order events - order placements, matches, and cancellations
     */
    private static void handleOrderEvent(Map<String, Object> messageMap) {
        OrderEvent event = (OrderEvent) messageMap.get("order_event");
        if (event == null) return;

        System.out.println("\n=== Order Event ===");
        System.out.println("Order ID: " + event.getId());
        System.out.println("Asset: " + event.getAssetId());
        System.out.println("Market: " + event.getMarket());
        System.out.printf("Order: %s @ %s (%s)%n",
                event.getOriginalSize(), event.getPrice(), event.getSide());
        System.out.println("Outcome: " + event.getOutcome());
        System.out.println("Owner: " + event.getOwner());
        System.out.println("Type: " + event.getType());

        // Display size information
        System.out.printf("Original Size: %s%n", event.getOriginalSize());
        System.out.printf("Size Matched: %s%n", event.getSizeMatched());
        System.out.printf("Remaining Size: %.4f%n", event.getRemainingSize());

        // Check order type
        if (event.isPlacement()) {
            System.out.println("Action: ORDER PLACED");
        } else if (event.isMatch()) {
            System.out.println("Action: ORDER MATCHED");
        } else if (event.isCancel()) {
            System.out.println("Action: ORDER CANCELLED");
        }

        // Check if fully matched
        if (event.isFullyMatched()) {
            System.out.println("Status: FULLY MATCHED ✓");
        } else {
            double fillPercentage = (event.getSizeMatchedAsDouble() / event.getOriginalSizeAsDouble()) * 100;
            System.out.printf("Fill: %.1f%%%n", fillPercentage);
        }

        // Check direction
        if (event.isBuy()) {
            System.out.println("Direction: BUY");
        } else if (event.isSell()) {
            System.out.println("Direction: SELL");
        }

        System.out.println("Timestamp: " + event.getTimestamp());
        System.out.println("===================\n");
    }
}

