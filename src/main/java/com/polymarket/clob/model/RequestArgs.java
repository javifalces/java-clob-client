package com.polymarket.clob.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request arguments for API calls
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestArgs {
    /**
     * HTTP method (GET, POST, DELETE, etc.)
     */
    private String method;
    
    /**
     * Request path/endpoint
     */
    private String requestPath;
    
    /**
     * Request body (can be null for GET requests)
     */
    private Object body;
    
    /**
     * Serialized body as string (optional)
     */
    private String serializedBody;
}
