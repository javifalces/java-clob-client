package com.polymarket.clob;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.polymarket.clob.config.Config;
import com.polymarket.clob.exception.PolyException;
import com.polymarket.clob.http.Headers;
import com.polymarket.clob.http.HttpClient;
import com.polymarket.clob.http.QueryBuilder;
import com.polymarket.clob.model.*;
import com.polymarket.clob.signing.Signer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static com.polymarket.clob.Constants.*;
import static com.polymarket.clob.Endpoints.*;

/**
 * Main client for interacting with the Polymarket CLOB
 * 
 * The client supports three modes:
 * - Level 0: No authentication - access to public endpoints only
 * - Level 1: Private key authentication - access to L1 endpoints
 * - Level 2: Full authentication with API credentials - access to all endpoints
 */
public class ClobClient {
    
    private static final Logger logger = LoggerFactory.getLogger(ClobClient.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private final String host;
    private final Integer chainId;
    private final Signer signer;
    private ApiCreds creds;
    private int mode;
    private final HttpClient httpClient;
    
    // Local caches
    private final Map<String, String> tickSizes = new HashMap<>();
    private final Map<String, Boolean> negRisk = new HashMap<>();
    private final Map<String, Integer> feeRates = new HashMap<>();
    
    /**
     * Create a new CLOB client
     * 
     * @param host The CLOB API host URL
     * @param chainId The chain ID (required for L1+ auth)
     * @param privateKey The private key (required for L1+ auth)
     * @param creds The API credentials (required for L2 auth)
     */
    public ClobClient(String host, Integer chainId, String privateKey, ApiCreds creds) {
        this.host = host.endsWith("/") ? host.substring(0, host.length() - 1) : host;
        this.chainId = chainId;
        this.signer = (privateKey != null && chainId != null) ? new Signer(privateKey, chainId) : null;
        this.creds = creds;
        this.mode = getClientMode();
        this.httpClient = new HttpClient();
    }
    
    /**
     * Create a Level 0 client (public endpoints only)
     */
    public ClobClient(String host) {
        this(host, null, null, null);
    }
    
    /**
     * Create a Level 1 client (with private key authentication)
     */
    public ClobClient(String host, int chainId, String privateKey) {
        this(host, chainId, privateKey, null);
    }
    
    // ==================== Address and Configuration Methods ====================
    
    /**
     * Get the public address of the signer
     */
    public String getAddress() {
        return signer != null ? signer.getAddress() : null;
    }
    
    /**
     * Get the collateral token address
     */
    public String getCollateralAddress() {
        if (chainId == null) return null;
        ContractConfig config = Config.getContractConfig(chainId);
        return config.getCollateral();
    }
    
    /**
     * Get the conditional token address
     */
    public String getConditionalAddress() {
        if (chainId == null) return null;
        ContractConfig config = Config.getContractConfig(chainId);
        return config.getConditionalTokens();
    }
    
    /**
     * Get the exchange address
     */
    public String getExchangeAddress(boolean negRisk) {
        if (chainId == null) return null;
        ContractConfig config = Config.getContractConfig(chainId, negRisk);
        return config.getExchange();
    }
    
    /**
     * Get the exchange address (standard, non-negative risk)
     */
    public String getExchangeAddress() {
        return getExchangeAddress(false);
    }
    
    // ==================== Health and Server Methods ====================
    
    /**
     * Health check - confirms server is up
     */
    public Object getOk() {
        return httpClient.get(host + "/");
    }
    
    /**
     * Get the current server time
     */
    public Object getServerTime() {
        return httpClient.get(host + TIME);
    }
    
    // ==================== API Key Management (Level 1+) ====================
    
    /**
     * Create a new CLOB API key
     */
    public ApiCreds createApiKey(long nonce) {
        assertLevel1Auth();
        
        String endpoint = host + CREATE_API_KEY;
        Map<String, String> headers = Headers.createLevel1Headers(signer, nonce);
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = (Map<String, Object>) httpClient.post(endpoint, headers);
            return new ApiCreds(
                (String) response.get("apiKey"),
                (String) response.get("secret"),
                (String) response.get("passphrase")
            );
        } catch (Exception e) {
            logger.error("Couldn't parse created CLOB creds", e);
            return null;
        }
    }
    
    /**
     * Create a new CLOB API key with default nonce
     */
    public ApiCreds createApiKey() {
        return createApiKey(0);
    }
    
    /**
     * Derive an existing CLOB API key
     */
    public ApiCreds deriveApiKey(long nonce) {
        assertLevel1Auth();
        
        String endpoint = host + DERIVE_API_KEY;
        Map<String, String> headers = Headers.createLevel1Headers(signer, nonce);
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = (Map<String, Object>) httpClient.get(endpoint, headers);
            return new ApiCreds(
                (String) response.get("apiKey"),
                (String) response.get("secret"),
                (String) response.get("passphrase")
            );
        } catch (Exception e) {
            logger.error("Couldn't parse derived CLOB creds", e);
            return null;
        }
    }
    
    /**
     * Derive an existing CLOB API key with default nonce
     */
    public ApiCreds deriveApiKey() {
        return deriveApiKey(0);
    }
    
    /**
     * Create API creds if not already created for nonce, otherwise derive them
     */
    public ApiCreds createOrDeriveApiCreds(long nonce) {
        try {
            return createApiKey(nonce);
        } catch (Exception e) {
            return deriveApiKey(nonce);
        }
    }
    
    /**
     * Create API creds if not already created, otherwise derive them (default nonce)
     */
    public ApiCreds createOrDeriveApiCreds() {
        return createOrDeriveApiCreds(0);
    }
    
    /**
     * Set the API credentials
     */
    public void setApiCreds(ApiCreds creds) {
        this.creds = creds;
        this.mode = getClientMode();
    }
    
    // ==================== API Key Management (Level 2+) ====================
    
    /**
     * Get available API keys for this address
     */
    public Object getApiKeys() {
        assertLevel2Auth();
        
        RequestArgs requestArgs = RequestArgs.builder()
            .method("GET")
            .requestPath(GET_API_KEYS)
            .build();
        Map<String, String> headers = Headers.createLevel2Headers(signer, creds, requestArgs);
        return httpClient.get(host + GET_API_KEYS, headers);
    }
    
    /**
     * Get the closed only mode flag for this address
     */
    public Object getClosedOnlyMode() {
        assertLevel2Auth();
        
        RequestArgs requestArgs = RequestArgs.builder()
            .method("GET")
            .requestPath(CLOSED_ONLY)
            .build();
        Map<String, String> headers = Headers.createLevel2Headers(signer, creds, requestArgs);
        return httpClient.get(host + CLOSED_ONLY, headers);
    }
    
    /**
     * Delete an API key
     */
    public Object deleteApiKey() {
        assertLevel2Auth();
        
        RequestArgs requestArgs = RequestArgs.builder()
            .method("DELETE")
            .requestPath(DELETE_API_KEY)
            .build();
        Map<String, String> headers = Headers.createLevel2Headers(signer, creds, requestArgs);
        return httpClient.delete(host + DELETE_API_KEY, headers);
    }
    
    /**
     * Create a readonly API key
     */
    public ReadonlyApiKeyResponse createReadonlyApiKey() {
        assertLevel2Auth();
        
        RequestArgs requestArgs = RequestArgs.builder()
            .method("POST")
            .requestPath(CREATE_READONLY_API_KEY)
            .build();
        Map<String, String> headers = Headers.createLevel2Headers(signer, creds, requestArgs);
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = (Map<String, Object>) httpClient.post(host + CREATE_READONLY_API_KEY, headers);
            return new ReadonlyApiKeyResponse((String) response.get("apiKey"));
        } catch (Exception e) {
            logger.error("Couldn't parse readonly API key response", e);
            return null;
        }
    }
    
    /**
     * Get available readonly API keys
     */
    public Object getReadonlyApiKeys() {
        assertLevel2Auth();
        
        RequestArgs requestArgs = RequestArgs.builder()
            .method("GET")
            .requestPath(GET_READONLY_API_KEYS)
            .build();
        Map<String, String> headers = Headers.createLevel2Headers(signer, creds, requestArgs);
        return httpClient.get(host + GET_READONLY_API_KEYS, headers);
    }
    
    /**
     * Delete a readonly API key
     */
    public Object deleteReadonlyApiKey(String key) {
        assertLevel2Auth();
        
        Map<String, String> body = Map.of("key", key);
        String serialized = serializeJson(body);
        
        RequestArgs requestArgs = RequestArgs.builder()
            .method("DELETE")
            .requestPath(DELETE_READONLY_API_KEY)
            .body(body)
            .serializedBody(serialized)
            .build();
        Map<String, String> headers = Headers.createLevel2Headers(signer, creds, requestArgs);
        return httpClient.delete(host + DELETE_READONLY_API_KEY, headers, serialized);
    }
    
    /**
     * Validate a readonly API key for a given address
     */
    public Object validateReadonlyApiKey(String address, String key) {
        return httpClient.get(String.format("%s%s?address=%s&key=%s", 
            host, VALIDATE_READONLY_API_KEY, address, key));
    }
    
    // ==================== Market Data Methods ====================
    
    /**
     * Get the mid market price for a token
     */
    public MidpointResponse getMidpoint(String tokenId) {
        Object response = httpClient.get(String.format("%s%s?token_id=%s", host, MID_POINT, tokenId));
        return objectMapper.convertValue(response, MidpointResponse.class);
    }
    
    /**
     * Get the market price for a token and side
     */
    public PriceResponse getPrice(String tokenId, String side) {
        Object response = httpClient.get(String.format("%s%s?token_id=%s&side=%s",
            host, PRICE, tokenId, side));
        return objectMapper.convertValue(response, PriceResponse.class);
    }
    
    /**
     * Get the spread for a token
     */
    public SpreadResponse getSpread(String tokenId) {
        Object response = httpClient.get(String.format("%s%s?token_id=%s", host, GET_SPREAD, tokenId));
        return objectMapper.convertValue(response, SpreadResponse.class);
    }
    
    /**
     * Get the last trade price for a token
     */
    public LastTradePriceResponse getLastTradePrice(String tokenId) {
        Object response = httpClient.get(String.format("%s%s?token_id=%s",
            host, GET_LAST_TRADE_PRICE, tokenId));
        return objectMapper.convertValue(response, LastTradePriceResponse.class);
    }
    
    /**
     * Get tick size for a token (with caching)
     */
    public String getTickSize(String tokenId) {
        if (tickSizes.containsKey(tokenId)) {
            return tickSizes.get(tokenId);
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) httpClient.get(
            String.format("%s%s?token_id=%s", host, GET_TICK_SIZE, tokenId));
        String tickSize = String.valueOf(result.get("minimum_tick_size"));
        tickSizes.put(tokenId, tickSize);
        
        return tickSize;
    }
    
    /**
     * Get negative risk flag for a token (with caching)
     */
    public boolean getNegRisk(String tokenId) {
        if (negRisk.containsKey(tokenId)) {
            return negRisk.get(tokenId);
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) httpClient.get(
            String.format("%s%s?token_id=%s", host, GET_NEG_RISK, tokenId));
        boolean isNegRisk = (Boolean) result.get("neg_risk");
        negRisk.put(tokenId, isNegRisk);
        
        return isNegRisk;
    }
    
    /**
     * Get fee rate in basis points for a token (with caching)
     */
    public int getFeeRateBps(String tokenId) {
        if (feeRates.containsKey(tokenId)) {
            return feeRates.get(tokenId);
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) httpClient.get(
            String.format("%s%s?token_id=%s", host, GET_FEE_RATE, tokenId));
        Object baseFee = result.get("base_fee");
        int feeRate = baseFee != null ? ((Number) baseFee).intValue() : 0;
        feeRates.put(tokenId, feeRate);
        
        return feeRate;
    }
    
    /**
     * Get order book for a token
     */
    public OrderBookResponse getOrderBook(String tokenId) {
        Object response = httpClient.get(String.format("%s%s?token_id=%s",
            host, GET_ORDER_BOOK, tokenId));
        return objectMapper.convertValue(response, OrderBookResponse.class);
    }
    
    // ==================== Order Management (Level 2+) ====================
    
    /**
     * Cancel an order
     */
    public Object cancel(String orderId) {
        assertLevel2Auth();
        
        Map<String, String> body = Map.of("orderID", orderId);
        String serialized = serializeJson(body);
        
        RequestArgs requestArgs = RequestArgs.builder()
            .method("DELETE")
            .requestPath(CANCEL)
            .body(body)
            .serializedBody(serialized)
            .build();
        Map<String, String> headers = Headers.createLevel2Headers(signer, creds, requestArgs);
        return httpClient.delete(host + CANCEL, headers, serialized);
    }
    
    /**
     * Cancel all orders
     */
    public Object cancelAll() {
        assertLevel2Auth();
        
        RequestArgs requestArgs = RequestArgs.builder()
            .method("DELETE")
            .requestPath(CANCEL_ALL)
            .build();
        Map<String, String> headers = Headers.createLevel2Headers(signer, creds, requestArgs);
        return httpClient.delete(host + CANCEL_ALL, headers);
    }
    
    /**
     * Get orders for the API key
     */
    public Object getOrders(OpenOrderParams params) {
        assertLevel2Auth();
        
        RequestArgs requestArgs = RequestArgs.builder()
            .method("GET")
            .requestPath(ORDERS)
            .build();
        Map<String, String> headers = Headers.createLevel2Headers(signer, creds, requestArgs);
        
        String url = QueryBuilder.addQueryOpenOrdersParams(host + ORDERS, params, "MA==");
        return httpClient.get(url, headers);
    }
    
    /**
     * Get an order by ID
     */
    public Object getOrder(String orderId) {
        assertLevel2Auth();
        
        String endpoint = GET_ORDER + orderId;
        RequestArgs requestArgs = RequestArgs.builder()
            .method("GET")
            .requestPath(endpoint)
            .build();
        Map<String, String> headers = Headers.createLevel2Headers(signer, creds, requestArgs);
        return httpClient.get(host + endpoint, headers);
    }
    
    /**
     * Get trades for the user
     */
    public Object getTrades(TradeParams params) {
        assertLevel2Auth();
        
        RequestArgs requestArgs = RequestArgs.builder()
            .method("GET")
            .requestPath(TRADES)
            .build();
        Map<String, String> headers = Headers.createLevel2Headers(signer, creds, requestArgs);
        
        String url = QueryBuilder.addQueryTradeParams(host + TRADES, params, "MA==");
        return httpClient.get(url, headers);
    }
    
    // ==================== Markets ====================
    
    /**
     * Get current markets
     */
    public Object getMarkets(String nextCursor) {
        return httpClient.get(String.format("%s%s?next_cursor=%s", 
            host, GET_MARKETS, nextCursor != null ? nextCursor : "MA=="));
    }
    
    /**
     * Get current markets (default cursor)
     */
    public Object getMarkets() {
        return getMarkets("MA==");
    }
    
    /**
     * Get a market by condition ID
     */
    public Object getMarket(String conditionId) {
        return httpClient.get(host + GET_MARKET + conditionId);
    }
    
    // ==================== Authentication Helpers ====================
    
    private void assertLevel1Auth() {
        if (mode < L1) {
            throw new PolyException(L1_AUTH_UNAVAILABLE);
        }
    }
    
    private void assertLevel2Auth() {
        if (mode < L2) {
            throw new PolyException(L2_AUTH_UNAVAILABLE);
        }
    }
    
    private int getClientMode() {
        if (signer != null && creds != null) {
            return L2;
        }
        if (signer != null) {
            return L1;
        }
        return L0;
    }
    
    /**
     * Serialize an object to JSON
     */
    private String serializeJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new PolyException("Failed to serialize JSON", e);
        }
    }
    
    // ==================== Getters ====================
    
    public String getHost() {
        return host;
    }
    
    public Integer getChainId() {
        return chainId;
    }
    
    public Signer getSigner() {
        return signer;
    }
    
    public ApiCreds getCreds() {
        return creds;
    }
    
    public int getMode() {
        return mode;
    }
}
