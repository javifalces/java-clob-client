package com.polymarket.clob.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.polymarket.clob.exception.PolyException;
import okhttp3.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * HTTP client utilities for making API requests
 */
public class HttpClient {
    
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private final OkHttpClient client;
    
    public HttpClient() {
        this.client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
    }
    
    /**
     * Make an HTTP request
     * 
     * @param endpoint The full URL endpoint
     * @param method The HTTP method
     * @param headers The headers (can be null)
     * @param data The request body data (can be null)
     * @return The response as a string or parsed object
     * @throws PolyException if the request fails
     */
    public Object request(String endpoint, String method, Map<String, String> headers, Object data) {
        try {
            Request.Builder requestBuilder = new Request.Builder().url(endpoint);
            
            // Add headers
            addDefaultHeaders(requestBuilder, method);
            if (headers != null) {
                headers.forEach(requestBuilder::addHeader);
            }
            
            // Build request body
            RequestBody body = null;
            if (data != null) {
                String json = data instanceof String ? (String) data : objectMapper.writeValueAsString(data);
                body = RequestBody.create(json, JSON);
            }
            
            // Set method
            switch (method.toUpperCase()) {
                case "GET":
                    requestBuilder.get();
                    break;
                case "POST":
                    requestBuilder.post(body != null ? body : RequestBody.create("", null));
                    break;
                case "DELETE":
                    if (body != null) {
                        requestBuilder.delete(body);
                    } else {
                        requestBuilder.delete();
                    }
                    break;
                case "PUT":
                    requestBuilder.put(body != null ? body : RequestBody.create("", null));
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported HTTP method: " + method);
            }
            
            Request request = requestBuilder.build();
            
            try (Response response = client.newCall(request).execute()) {
                String responseBody = readBody(response);

                if (!response.isSuccessful()) {
                    throw new PolyException("HTTP " + response.code() + ": " + responseBody);
                }
                
                // Try to parse as JSON, otherwise return as string
                try {
                    return objectMapper.readValue(responseBody, Object.class);
                } catch (Exception e) {
                    return responseBody;
                }
            }
            
        } catch (IOException e) {
            throw new PolyException("Request failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Read the response body, decompressing gzip if indicated by Content-Encoding.
     * Required because we manually set Accept-Encoding: gzip, which disables OkHttp's
     * transparent decompression.
     */
    private String readBody(Response response) throws IOException {
        ResponseBody body = response.body();
        if (body == null) return "";
        String encoding = response.header("Content-Encoding");
        if ("gzip".equalsIgnoreCase(encoding)) {
            try (java.util.zip.GZIPInputStream gzip =
                         new java.util.zip.GZIPInputStream(body.byteStream())) {
                return new String(gzip.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
        }
        return body.string();
    }

    /**
     * Add default headers to the request
     */
    private void addDefaultHeaders(Request.Builder requestBuilder, String method) {
        requestBuilder.addHeader("User-Agent", "java-clob-client");
        requestBuilder.addHeader("Accept", "*/*");
        requestBuilder.addHeader("Connection", "keep-alive");
        requestBuilder.addHeader("Content-Type", "application/json");
        
        if ("GET".equalsIgnoreCase(method)) {
            requestBuilder.addHeader("Accept-Encoding", "gzip");
        }
    }
    
    /**
     * Make a GET request
     */
    public Object get(String endpoint, Map<String, String> headers) {
        return request(endpoint, "GET", headers, null);
    }
    
    /**
     * Make a GET request without headers
     */
    public Object get(String endpoint) {
        return get(endpoint, null);
    }
    
    /**
     * Make a POST request
     */
    public Object post(String endpoint, Map<String, String> headers, Object data) {
        return request(endpoint, "POST", headers, data);
    }
    
    /**
     * Make a POST request without data
     */
    public Object post(String endpoint, Map<String, String> headers) {
        return post(endpoint, headers, null);
    }
    
    /**
     * Make a DELETE request
     */
    public Object delete(String endpoint, Map<String, String> headers, Object data) {
        return request(endpoint, "DELETE", headers, data);
    }
    
    /**
     * Make a DELETE request without data
     */
    public Object delete(String endpoint, Map<String, String> headers) {
        return delete(endpoint, headers, null);
    }
    
    /**
     * Make a PUT request
     */
    public Object put(String endpoint, Map<String, String> headers, Object data) {
        return request(endpoint, "PUT", headers, data);
    }
}
