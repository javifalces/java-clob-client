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
        String tokenId = "42139849929574046088630785796780813725435914859433767469767950066058132350666";
        
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
            OrderBookResponse orderBook = client.getOrderBook(tokenId);
            System.out.println(orderBook);
            System.out.println("Best bid: " + orderBook.getBestBidPrice() + " @ " + orderBook.getBestBidSize());
            System.out.println("Best ask: " + orderBook.getBestAskPrice() + " @ " + orderBook.getBestAskSize());
            System.out.println("Spread: " + orderBook.getSpread());
            System.out.println("Mid: " + orderBook.getMidPrice());

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
