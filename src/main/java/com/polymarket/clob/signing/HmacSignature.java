package com.polymarket.clob.signing;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * HMAC signature builder for API authentication
 */
public class HmacSignature {
    
    private static final String HMAC_SHA256 = "HmacSHA256";
    
    /**
     * Build an HMAC signature for API authentication
     * 
     * @param secret The API secret (base64 encoded)
     * @param timestamp The timestamp
     * @param method The HTTP method
     * @param requestPath The request path
     * @param body The request body (can be null)
     * @return Base64-encoded HMAC signature
     */
    public static String buildHmacSignature(String secret, long timestamp, String method, 
                                           String requestPath, String body) {
        try {
            // Decode the base64 secret
            byte[] decodedSecret = Base64.getUrlDecoder().decode(secret);
            
            // Build the message to sign
            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append(timestamp);
            messageBuilder.append(method);
            messageBuilder.append(requestPath);
            
            if (body != null && !body.isEmpty()) {
                messageBuilder.append(body);
            }
            
            String message = messageBuilder.toString();
            
            // Create HMAC SHA256
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(decodedSecret, HMAC_SHA256);
            mac.init(secretKeySpec);
            
            // Sign the message
            byte[] signedBytes = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            
            // Return base64 encoded signature
            return Base64.getUrlEncoder().encodeToString(signedBytes);
            
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to create HMAC signature", e);
        }
    }
}
