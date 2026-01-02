package com.polymarket.clob.http;

import com.polymarket.clob.model.ApiCreds;
import com.polymarket.clob.model.RequestArgs;
import com.polymarket.clob.signing.Eip712;
import com.polymarket.clob.signing.HmacSignature;
import com.polymarket.clob.signing.Signer;

import java.util.HashMap;
import java.util.Map;

/**
 * Utilities for creating authentication headers
 */
public class Headers {
    
    // Header names
    public static final String POLY_ADDRESS = "POLY_ADDRESS";
    public static final String POLY_SIGNATURE = "POLY_SIGNATURE";
    public static final String POLY_TIMESTAMP = "POLY_TIMESTAMP";
    public static final String POLY_NONCE = "POLY_NONCE";
    public static final String POLY_API_KEY = "POLY_API_KEY";
    public static final String POLY_PASSPHRASE = "POLY_PASSPHRASE";
    
    /**
     * Create Level 1 authentication headers (EIP-712 signature)
     * 
     * @param signer The signer
     * @param nonce The nonce (use 0 for default)
     * @return Headers map
     */
    public static Map<String, String> createLevel1Headers(Signer signer, long nonce) {
        long timestamp = System.currentTimeMillis() / 1000;
        
        String signature = Eip712.signClobAuthMessage(signer, timestamp, nonce);
        
        Map<String, String> headers = new HashMap<>();
        headers.put(POLY_ADDRESS, signer.getAddress());
        headers.put(POLY_SIGNATURE, signature);
        headers.put(POLY_TIMESTAMP, String.valueOf(timestamp));
        headers.put(POLY_NONCE, String.valueOf(nonce));
        
        return headers;
    }
    
    /**
     * Create Level 1 authentication headers with default nonce (0)
     * 
     * @param signer The signer
     * @return Headers map
     */
    public static Map<String, String> createLevel1Headers(Signer signer) {
        return createLevel1Headers(signer, 0);
    }
    
    /**
     * Create Level 2 authentication headers (HMAC signature)
     * 
     * @param signer The signer
     * @param creds The API credentials
     * @param requestArgs The request arguments
     * @return Headers map
     */
    public static Map<String, String> createLevel2Headers(Signer signer, ApiCreds creds, 
                                                          RequestArgs requestArgs) {
        long timestamp = System.currentTimeMillis() / 1000;
        
        // Use pre-serialized body if available for deterministic signing
        String bodyForSig = requestArgs.getSerializedBody() != null 
            ? requestArgs.getSerializedBody() 
            : (requestArgs.getBody() != null ? requestArgs.getBody().toString() : null);
        
        String hmacSig = HmacSignature.buildHmacSignature(
            creds.getApiSecret(),
            timestamp,
            requestArgs.getMethod(),
            requestArgs.getRequestPath(),
            bodyForSig
        );
        
        Map<String, String> headers = new HashMap<>();
        headers.put(POLY_ADDRESS, signer.getAddress());
        headers.put(POLY_SIGNATURE, hmacSig);
        headers.put(POLY_TIMESTAMP, String.valueOf(timestamp));
        headers.put(POLY_API_KEY, creds.getApiKey());
        headers.put(POLY_PASSPHRASE, creds.getApiPassphrase());
        
        return headers;
    }
    
    /**
     * Enrich Level 2 headers with builder headers
     * 
     * @param headers The base headers
     * @param builderHeaders The builder headers to add
     * @return Combined headers map
     */
    public static Map<String, String> enrichL2HeadersWithBuilderHeaders(
            Map<String, String> headers, Map<String, String> builderHeaders) {
        Map<String, String> enriched = new HashMap<>(headers);
        enriched.putAll(builderHeaders);
        return enriched;
    }
}
