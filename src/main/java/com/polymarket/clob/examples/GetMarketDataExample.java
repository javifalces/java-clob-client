package com.polymarket.clob.examples;

import com.polymarket.clob.ClobClient;
import com.polymarket.clob.model.*;

/**
 * Example: Get market data
 */
public class GetMarketDataExample {
    
    public static void main(String[] args) {
        // Create a Level 0 client (no authentication needed for public data)
        ClobClient client = new ClobClient("https://clob.polymarket.com");
        
        // Example token ID (replace with actual token ID)
        String tokenId = "11862165566757345985240476164489718219056735011698825377388402888080786399275";
        
        try {
            // Get midpoint price
            System.out.println("Midpoint price:");
            MidpointResponse midpoint = client.getMidpoint(tokenId);
            System.out.println(midpoint);
            System.out.println("Mid: " + midpoint.getMid());

            // Get spread
            System.out.println("\nSpread:");
            SpreadResponse spread = client.getSpread(tokenId);
            System.out.println(spread);
            System.out.println("Spread: " + spread.getSpread());

            // Get last trade price
            System.out.println("\nLast trade price:");
            LastTradePriceResponse lastPrice = client.getLastTradePrice(tokenId);
            System.out.println(lastPrice);
            System.out.println("Price: " + lastPrice.getPrice());

            // Get order book
            System.out.println("\nOrder book:");
            BookEvent orderBook = client.getOrderBook(tokenId);
            System.out.println(orderBook);
            if (orderBook.getBids().isEmpty() && orderBook.getAsks().isEmpty()) {
                System.out.println("Order book is empty.");
                return;
            }
            if (orderBook.getBids().isEmpty()) {
                System.out.println("No bids in the order book.");
                return;
            } else {
                System.out.println("Best bid: " + orderBook.getBestBid().getPriceAsDouble() + " @ " + orderBook.getBestBid().getSizeAsDouble());
            }
            if (orderBook.getAsks().isEmpty()) {
                System.out.println("No asks in the order book.");
                return;
            } else {
                System.out.println("Best ask: " + orderBook.getBestAsk().getPriceAsDouble() + " @ " + orderBook.getBestAsk().getSizeAsDouble());
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
