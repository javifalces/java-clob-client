package com.polymarket.clob.examples;

import com.polymarket.clob.ClobClient;

/**
 * Example: Get market data
 */
public class GetMarketDataExample {
    
    public static void main(String[] args) {
        // Create a Level 0 client (no authentication needed for public data)
        ClobClient client = new ClobClient("https://clob.polymarket.com");
        
        // Example token ID (replace with actual token ID)
        String tokenId = "21742633143463906290569050155826241533067272736897614950488156847949938836455";
        
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
