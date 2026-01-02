package com.polymarket.clob.examples;

import com.polymarket.clob.ClobClient;

/**
 * Example: Check server health and time
 */
public class GetOkExample {
    
    public static void main(String[] args) {
        // Create a Level 0 client (no authentication)
        ClobClient client = new ClobClient("https://clob.polymarket.com");
        
        // Check server health
        System.out.println("Server health check:");
        Object health = client.getOk();
        System.out.println(health);
        
        // Get server time
        System.out.println("\nServer time:");
        Object time = client.getServerTime();
        System.out.println(time);
    }
}
