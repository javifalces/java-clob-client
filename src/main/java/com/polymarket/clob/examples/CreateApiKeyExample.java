package com.polymarket.clob.examples;

import com.polymarket.clob.ClobClient;
import com.polymarket.clob.model.ApiCreds;

/**
 * Example: Create API credentials
 */
public class CreateApiKeyExample {
    
    public static void main(String[] args) {
        // Get private key from environment variable
        String privateKey = System.getenv("PRIVATE_KEY");
        if (privateKey == null) {
            System.err.println("Please set PRIVATE_KEY environment variable");
            System.exit(1);
        }
        
        int chainId = 137; // Polygon mainnet
        
        // Create a Level 1 client
        ClobClient client = new ClobClient(
            "https://clob.polymarket.com",
            chainId,
            privateKey
        );
        
        System.out.println("Creating API key for address: " + client.getAddress());
        
        // Create API key
        ApiCreds creds = client.createApiKey();
        
        if (creds != null) {
            System.out.println("\n🚨🚨🚨");
            System.out.println("API Key created successfully!");
            System.out.println("Your credentials CANNOT be recovered after they've been created.");
            System.out.println("Be sure to store them safely!");
            System.out.println("🚨🚨🚨\n");
            
            System.out.println("API Key: " + creds.getApiKey());
            System.out.println("API Secret: " + creds.getApiSecret());
            System.out.println("API Passphrase: " + creds.getApiPassphrase());
        } else {
            System.err.println("Failed to create API key");
        }
    }
}
