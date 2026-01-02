package com.polymarket.clob.examples;

import com.polymarket.clob.ClobClient;

/**
 * Example: Get markets information
 */
public class GetMarketsExample {
    
    public static void main(String[] args) {
        // Create a Level 0 client
        ClobClient client = new ClobClient("https://clob.polymarket.com");
        
        try {
            // Get all markets
            System.out.println("Fetching markets...");
            Object markets = client.getMarkets();
            System.out.println(markets);
            
            // Get specific market by condition ID (replace with actual ID)
            // String conditionId = "0x123...";
            // Object market = client.getMarket(conditionId);
            // System.out.println("\nSpecific market:");
            // System.out.println(market);
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
