package com.polymarket.clob.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API Credentials for Level 2 authentication
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiCreds {
    /**
     * API key
     */
    private String apiKey;
    
    /**
     * API secret
     */
    private String apiSecret;
    
    /**
     * API passphrase
     */
    private String apiPassphrase;
}
