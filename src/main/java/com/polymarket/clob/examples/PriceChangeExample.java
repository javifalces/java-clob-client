package com.polymarket.clob.examples;

import com.polymarket.clob.model.PriceChangeEvent;
import com.polymarket.clob.model.PriceChangeEntry;
import com.polymarket.clob.websocket.WebSocketClobClient;
import com.polymarket.clob.websocket.WebSocketListener;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Example demonstrating how to listen for price_change events from the WebSocket stream.
 */
public class PriceChangeExample {

    public static void main(String[] args) throws InterruptedException {
        // Asset IDs to subscribe to
        List<String> assetIds = Arrays.asList(
                "71478852790279095447182996049071040792010759617668969799049179229104800573786",
                "11862165566757345985240476164489718219056735011698825377388402888080786399275"
        );

        // Create WebSocket client for market channel
        String baseUrl = "wss://ws-subscriptions-clob.polymarket.com";
        WebSocketClobClient client = new WebSocketClobClient(
                WebSocketClobClient.MARKET_CHANNEL,
                baseUrl,
                assetIds,
                null  // No auth needed for market channel
        );

        // Register a listener to handle price_change events
        client.registerListener(new WebSocketListener() {
            @Override
            public void onEvent(String eventType, Map<String, Object> messageMap) {
                if ("price_change".equals(eventType)) {
                    // Get the deserialized PriceChangeEvent object
                    PriceChangeEvent event = (PriceChangeEvent) messageMap.get("price_change_event");

                    if (event != null) {
                        System.out.println("=== Price Change Event ===");
                        System.out.println("Market: " + event.getMarket());
                        System.out.println("Timestamp: " + event.getTimestamp());

                        // Process each price change
                        for (PriceChangeEntry priceChange : event.getPriceChanges()) {
                            System.out.println("\n  Asset: " + priceChange.getAssetId());
                            System.out.println("  Price: " + priceChange.getPrice());
                            System.out.println("  Size: " + priceChange.getSize());
                            System.out.println("  Side: " + priceChange.getSide());
                            System.out.println("  Best Bid: " + priceChange.getBestBid());
                            System.out.println("  Best Ask: " + priceChange.getBestAsk());
                            System.out.println("  Hash: " + priceChange.getHash());
                        }
                        System.out.println("========================\n");
                    }
                } else {
                    // Handle other event types
                    System.out.println("Received event: " + eventType);
                }
            }
        });

        // Connect and start listening
        client.run();

        // Keep the program running (use Ctrl+C to stop)
        Thread.sleep(Long.MAX_VALUE);
    }
}

