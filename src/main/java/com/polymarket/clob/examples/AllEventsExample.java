package com.polymarket.clob.examples;

import com.polymarket.clob.model.*;
import com.polymarket.clob.websocket.WebSocketClobClient;
import com.polymarket.clob.websocket.WebSocketListener;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive example demonstrating how to listen for all deserialized event types
 * from the WebSocket stream: book, price_change, last_trade_price, and best_bid_ask.
 */
public class AllEventsExample {

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

        // Register a listener to handle all event types
        client.registerListener(new WebSocketListener() {
            @Override
            public void onEvent(String eventType, Map<String, Object> messageMap) {
                switch (eventType) {
                    case "book":
                        handleBookEvent(messageMap);
                        break;

                    case "price_change":
                        handlePriceChangeEvent(messageMap);
                        break;

                    case "last_trade_price":
                        handleLastTradePriceEvent(messageMap);
                        break;

                    case "best_bid_ask":
                        handleBestBidAskEvent(messageMap);
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
     * Handle book events - full order book snapshots
     */
    private static void handleBookEvent(Map<String, Object> messageMap) {
        BookEvent event = (BookEvent) messageMap.get("book_event");
        if (event == null) return;

        System.out.println("\n=== Book Event ===");
        System.out.println("Asset: " + event.getAssetId());
        System.out.println("Market: " + event.getMarket());
        System.out.println("Timestamp: " + event.getTimestamp());

        System.out.println("\nBids (" + event.getBids().size() + "):");
        for (OrderBookEntry bid : event.getBids()) {
            System.out.printf("  %s @ %s%n", bid.getSize(), bid.getPrice());
        }

        System.out.println("\nAsks (" + event.getAsks().size() + "):");
        for (OrderBookEntry ask : event.getAsks()) {
            System.out.printf("  %s @ %s%n", ask.getSize(), ask.getPrice());
        }

        OrderBookEntry bestBid = event.getBestBid();
        OrderBookEntry bestAsk = event.getBestAsk();
        if (bestBid != null && bestAsk != null) {
            double spread = bestAsk.getPriceAsDouble() - bestBid.getPriceAsDouble();
            System.out.printf("\nBest Bid: %s, Best Ask: %s, Spread: %.4f%n",
                    bestBid.getPrice(), bestAsk.getPrice(), spread);
        }
        System.out.println("==================\n");
    }

    /**
     * Handle price_change events - price updates for assets
     */
    private static void handlePriceChangeEvent(Map<String, Object> messageMap) {
        PriceChangeEvent event = (PriceChangeEvent) messageMap.get("price_change_event");
        if (event == null) return;

        System.out.println("\n=== Price Change Event ===");
        System.out.println("Market: " + event.getMarket());
        System.out.println("Timestamp: " + event.getTimestamp());

        for (PriceChangeEntry change : event.getPriceChanges()) {
            System.out.println("\n  Asset: " + change.getAssetId());
            System.out.println("  Price: " + change.getPrice() + " (" + change.getSide() + ")");
            System.out.println("  Size: " + change.getSize());
            System.out.println("  Best Bid: " + change.getBestBid());
            System.out.println("  Best Ask: " + change.getBestAsk());

            double midPrice = (change.getBestBidAsDouble() + change.getBestAskAsDouble()) / 2.0;
            System.out.printf("  Mid Price: %.4f%n", midPrice);
        }
        System.out.println("==========================\n");
    }

    /**
     * Handle last_trade_price events - most recent trade executions
     */
    private static void handleLastTradePriceEvent(Map<String, Object> messageMap) {
        LastTradePriceEvent event = (LastTradePriceEvent) messageMap.get("last_trade_price_event");
        if (event == null) return;

        System.out.println("\n=== Last Trade Price Event ===");
        System.out.println("Asset: " + event.getAssetId());
        System.out.println("Market: " + event.getMarket());
        System.out.printf("Trade: %s @ %s (%s)%n",
                event.getSize(), event.getPrice(), event.getSide());
        System.out.println("Fee Rate (bps): " + event.getFeeRateBps());
        System.out.println("Timestamp: " + event.getTimestamp());

        double totalValue = event.getPriceAsDouble() * event.getSizeAsDouble();
        System.out.printf("Total Value: %.2f%n", totalValue);
        System.out.println("==============================\n");
    }

    /**
     * Handle best_bid_ask events - top of book updates
     */
    private static void handleBestBidAskEvent(Map<String, Object> messageMap) {
        BestBidAskEvent event = (BestBidAskEvent) messageMap.get("best_bid_ask_event");
        if (event == null) return;

        System.out.println("\n=== Best Bid/Ask Event ===");
        System.out.println("Asset: " + event.getAssetId());
        System.out.println("Market: " + event.getMarket());
        System.out.println("Best Bid: " + event.getBestBid());
        System.out.println("Best Ask: " + event.getBestAsk());
        System.out.println("Spread: " + event.getSpread());
        System.out.printf("Mid Price: %.4f%n", event.getMidPrice());
        System.out.println("Timestamp: " + event.getTimestamp());

        double spreadBps = (event.getSpreadAsDouble() / event.getMidPrice()) * 10000;
        System.out.printf("Spread (bps): %.2f%n", spreadBps);
        System.out.println("==========================\n");
    }
}

