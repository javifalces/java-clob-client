package com.polymarket.clob.examples;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.polymarket.clob.ClobClient;

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
            Object midpoint = client.getMidpoint(tokenId);
            System.out.println(midpoint);
            
            // Get spread
            System.out.println("\nSpread:");
            Object spread = client.getSpread(tokenId);
            System.out.println(spread);
            
            // Get last trade price
            System.out.println("\nLast trade price:");
            Object lastPrice = client.getLastTradePrice(tokenId);
            System.out.println(lastPrice);
            
            // Get order book
            System.out.println("\nOrder book:");
            Object orderBook = client.getOrderBook(tokenId);
            System.out.println(orderBook);
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
