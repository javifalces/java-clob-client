package com.polymarket.clob;

import com.alibaba.fastjson2.JSON;
import com.polymarket.clob.config.Config;
import com.polymarket.clob.exception.PolyException;
import com.polymarket.clob.http.Headers;
import com.polymarket.clob.http.HttpClient;
import com.polymarket.clob.http.QueryBuilder;
import com.polymarket.clob.model.*;
import com.polymarket.clob.signing.OrderBuilder;
import com.polymarket.clob.signing.Signer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.polymarket.clob.Constants.*;
import static com.polymarket.clob.Endpoints.*;
import static org.web3j.utils.Strings.isEmpty;

/**
 * Main client for interacting with the Polymarket CLOB
 * <p>
 * The client supports three modes:
 * - Level 0: No authentication - access to public endpoints only
 * - Level 1: Private key authentication - access to L1 endpoints
 * - Level 2: Full authentication with API credentials - access to all endpoints
 */
public class ClobClient {

    private static final Logger logger = LogManager.getLogger(ClobClient.class);

    private final String host;
    private final Integer chainId;
    private final Signer signer;
    private final OrderBuilder builder;
    private ApiCreds creds;
    private int mode;
    private final HttpClient httpClient;
    private final Integer signatureType;
    private final String funder;

    // Local caches
    private final Map<String, String> tickSizes = new HashMap<>();
    private final Map<String, Boolean> negRisk = new HashMap<>();
    private final Map<String, Integer> feeRates = new HashMap<>();
    private Integer cachedVersion = null;

    /**
     * Create a new CLOB client
     *
     * @param host          The CLOB API host URL
     * @param chainId       The chain ID (required for L1+ auth)
     * @param privateKey    The private key (required for L1+ auth)
     * @param creds         The API credentials (required for L2 auth)
     * @param signatureType The signature type (0 for EOA, 1 for Poly Proxy, 2 for Poly Gnosis Safe)
     * @param funder        The funder address (optional, defaults to signer address)
     */
    public ClobClient(String host, Integer chainId, String privateKey, ApiCreds creds, Integer signatureType, String funder) {
        this.host = host.endsWith("/") ? host.substring(0, host.length() - 1) : host;
        this.chainId = chainId;
        this.signatureType = signatureType;
        this.funder = funder;
        this.signer = (privateKey != null && chainId != null) ? new Signer(privateKey, chainId) : null;
        this.builder = (this.signer != null)
                ? new OrderBuilder(this.signer, signatureType != null ? signatureType : OrderBuilder.SignatureType.EOA.getValue(), funder)
                : null;
        this.creds = creds;
        this.mode = getClientMode();
        this.httpClient = new HttpClient();
    }

    /**
     * Create a Level 0 client (public endpoints only)
     */
    public ClobClient(String host) {
        this(host, null, null, null, null, null);
    }

    /**
     * Create a Level 1 client (with private key authentication)
     */
    public ClobClient(String host, int chainId, String privateKey) {
        this(host, chainId, privateKey, null, null, null);
    }

    /**
     * Create a Level 2 client (with private key and API credentials)
     */
    public ClobClient(String host, Integer chainId, String privateKey, ApiCreds creds) {
        this(host, chainId, privateKey, creds, null, null);
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
        return httpClient.get(host + OK);
    }

    /**
     * Get the current server time
     */
    public Object getServerTime() {
        return httpClient.get(host + TIME);
    }

    /**
     * Get the current order version the server expects (1 = V1 legacy, 2 = V2).
     * The result is cached for the lifetime of the client.
     */
    public int getVersion() {
        if (cachedVersion != null) {
            return cachedVersion;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) httpClient.get(host + VERSION);
            Object v = result.get("version");
            cachedVersion = v != null ? ((Number) v).intValue() : 2;
        } catch (Exception e) {
            logger.warn("Failed to fetch server version, defaulting to V2", e);
            cachedVersion = 2;
        }
        return cachedVersion;
    }

    /**
     * Invalidate the cached server version so the next call to getVersion() re-fetches it.
     */
    public void invalidateVersionCache() {
        cachedVersion = null;
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
        return JSON.to(MidpointResponse.class, response);
    }

    /**
     * Get the market price for a token and side
     */
    public PriceResponse getPrice(String tokenId, String side) {
        Object response = httpClient.get(String.format("%s%s?token_id=%s&side=%s",
                host, PRICE, tokenId, side));
        return JSON.to(PriceResponse.class, response);
    }

    /**
     * Get the spread for a token
     */
    public SpreadResponse getSpread(String tokenId) {
        Object response = httpClient.get(String.format("%s%s?token_id=%s", host, GET_SPREAD, tokenId));
        return JSON.to(SpreadResponse.class, response);
    }

    /**
     * Get the last trade price for a token
     */
    public LastTradePriceResponse getLastTradePrice(String tokenId) {
        Object response = httpClient.get(String.format("%s%s?token_id=%s",
                host, GET_LAST_TRADE_PRICE, tokenId));
        return JSON.to(LastTradePriceResponse.class, response);
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
    public BookEvent getOrderBook(String tokenId) {
        Object response = httpClient.get(String.format("%s%s?token_id=%s",
                host, GET_ORDER_BOOK, tokenId));
        return JSON.to(BookEvent.class, response);
    }

    // ==================== Order Management (Level 2+) ====================

    /**
     * Create and sign a V2 order (Level 1 Auth required).
     * This is the default order creation method, using the V2 exchange contract.
     *
     * @param orderArgs The order arguments
     * @param options   The creation options (optional)
     * @return A signed V2 order ready to post
     */
    public SignedOrderV2 createOrder(OrderArgs orderArgs, CreateOrderOptions options) {
        assertLevel1Auth();

        String tickSize = resolveTickSize(orderArgs.getTokenId(),
                options != null ? options.getTickSize() : null);

        if (!isPriceValid(orderArgs.getPrice(), tickSize)) {
            throw new PolyException(String.format(
                    "Invalid price (%f), min: %s - max: %s",
                    orderArgs.getPrice(), tickSize, (1 - Double.parseDouble(tickSize))
            ));
        }

        boolean isNegRisk = (options != null && options.isNegRisk())
                ? options.isNegRisk()
                : getNegRisk(orderArgs.getTokenId());

        CreateOrderOptions resolvedOptions = CreateOrderOptions.builder()
                .tickSize(tickSize)
                .negRisk(isNegRisk)
                .build();

        return builder.createOrderV2(orderArgs, resolvedOptions);
    }

    /**
     * Create and sign a V2 order with default options
     */
    public SignedOrderV2 createOrder(OrderArgs orderArgs) {
        return createOrder(orderArgs, null);
    }

    /**
     * Create and sign a V2 market order (Level 1 Auth required).
     *
     * @param orderArgs The market order arguments
     * @param options   The creation options (optional)
     * @return A signed V2 order ready to post
     */
    public SignedOrderV2 createMarketOrder(MarketOrderArgs orderArgs, CreateOrderOptions options) {
        assertLevel1Auth();

        String tickSize = resolveTickSize(orderArgs.getTokenId(),
                options != null ? options.getTickSize() : null);

        if (orderArgs.getPrice() <= 0) {
            orderArgs.setPrice(calculateMarketPrice(
                    orderArgs.getTokenId(),
                    orderArgs.getSide(),
                    orderArgs.getAmount()
            ));
        }

        if (!isPriceValid(orderArgs.getPrice(), tickSize)) {
            throw new PolyException(String.format(
                    "Invalid price (%f), min: %s - max: %s",
                    orderArgs.getPrice(), tickSize, (1 - Double.parseDouble(tickSize))
            ));
        }

        boolean isNegRisk = (options != null && options.isNegRisk())
                ? options.isNegRisk()
                : getNegRisk(orderArgs.getTokenId());

        CreateOrderOptions resolvedOptions = CreateOrderOptions.builder()
                .tickSize(tickSize)
                .negRisk(isNegRisk)
                .build();

        return builder.createMarketOrderV2(orderArgs, resolvedOptions);
    }

    /**
     * Create and sign a V2 market order with default options
     */
    public SignedOrderV2 createMarketOrder(MarketOrderArgs orderArgs) {
        return createMarketOrder(orderArgs, null);
    }

    /**
     * Post a signed V2 order to the exchange
     *
     * @param order     The signed V2 order
     * @param orderType The order type (GTC, FOK, etc.)
     * @param postOnly  Whether this is a post-only order
     * @return OrderResponse with the result
     */
    public OrderResponse postOrder(SignedOrderV2 order, OrderType orderType, boolean postOnly) {
        assertLevel2Auth();

        if (postOnly && (orderType == OrderType.FOK || orderType == OrderType.FAK)) {
            throw new PolyException("post_only is not supported for FOK/FAK orders");
        }

        Map<String, Object> body = orderToJsonV2(order, creds.getApiKey(), orderType, postOnly);
        String serialized = serializeJson(body);

        RequestArgs requestArgs = RequestArgs.builder()
                .method("POST")
                .requestPath(POST_ORDER)
                .body(serialized)
                .serializedBody(serialized)
                .build();

        Map<String, String> headers = Headers.createLevel2Headers(signer, creds, requestArgs);
        Object response = httpClient.post(host + POST_ORDER, headers, serialized);
        return JSON.to(OrderResponse.class, response);
    }

    /**
     * Post a signed V2 order with default type (GTC)
     */
    public OrderResponse postOrder(SignedOrderV2 order) {
        return postOrder(order, OrderType.GTC, false);
    }

    /**
     * Post a signed V1 (legacy) order to the exchange
     *
     * @param order     The signed V1 order
     * @param orderType The order type (GTC, FOK, etc.)
     * @param postOnly  Whether this is a post-only order
     * @return OrderResponse with the result
     */
    public OrderResponse postOrder(SignedOrder order, OrderType orderType, boolean postOnly) {
        assertLevel2Auth();

        if (postOnly && orderType != OrderType.GTC && orderType != OrderType.GTD) {
            throw new PolyException("post_only orders can only be of type GTC or GTD");
        }

        Map<String, Object> body = orderToJson(order, creds.getApiKey(), orderType, postOnly);
        String serialized = serializeJson(body);

        RequestArgs requestArgs = RequestArgs.builder()
                .method("POST")
                .requestPath(POST_ORDER)
                .body(serialized)
                .serializedBody(serialized)
                .build();

        Map<String, String> headers = Headers.createLevel2Headers(signer, creds, requestArgs);
        Object response = httpClient.post(host + POST_ORDER, headers, serialized);
        return JSON.to(OrderResponse.class, response);
    }

    /**
     * Post a signed V1 (legacy) order with default type (GTC)
     */
    public OrderResponse postOrder(SignedOrder order) {
        return postOrder(order, OrderType.GTC, false);
    }

    /**
     * Post multiple signed V2 orders to the exchange
     *
     * @param orders List of order arguments with their configurations
     * @return List of OrderResponse with the results
     */
    public List<OrderResponse> postOrders(List<PostOrdersArgs> orders) {
        assertLevel2Auth();

        List<Map<String, Object>> body = orders.stream()
                .map(arg -> orderToJsonV2(arg.getOrder(), creds.getApiKey(),
                        arg.getOrderType(), arg.isPostOnly()))
                .collect(Collectors.toList());

        String serialized = serializeJson(body);

        RequestArgs requestArgs = RequestArgs.builder()
                .method("POST")
                .requestPath(POST_ORDERS)
                .body(body)
                .serializedBody(serialized)
                .build();

        Map<String, String> headers = Headers.createLevel2Headers(signer, creds, requestArgs);
        Object response = httpClient.post(host + POST_ORDERS, headers, serialized);

        @SuppressWarnings("unchecked")
        List<Object> responseList = (List<Object>) response;
        return responseList.stream()
                .map(obj -> JSON.to(OrderResponse.class, obj))
                .collect(Collectors.toList());
    }

    /**
     * Create and post a V2 order in one step
     *
     * @param orderArgs The order arguments
     * @param options   The creation options (optional)
     * @return OrderResponse with the result
     */
    public OrderResponse createAndPostOrder(OrderArgs orderArgs, CreateOrderOptions options) {
        SignedOrderV2 order = createOrder(orderArgs, options);
        return postOrder(order);
    }

    /**
     * Create and post a V2 order with default options
     */
    public OrderResponse createAndPostOrder(OrderArgs orderArgs) {
        return createAndPostOrder(orderArgs, null);
    }

    /**
     * Cancel multiple orders
     *
     * @param orderIds List of order IDs to cancel
     * @return CancelOrdersResponse with the results
     */
    public CancelOrdersResponse cancelOrders(List<String> orderIds) {
        assertLevel2Auth();

        String serialized = serializeJson(orderIds);

        RequestArgs requestArgs = RequestArgs.builder()
                .method("DELETE")
                .requestPath(CANCEL_ORDERS)
                .body(orderIds)
                .serializedBody(serialized)
                .build();

        Map<String, String> headers = Headers.createLevel2Headers(signer, creds, requestArgs);
        Object response = httpClient.delete(host + CANCEL_ORDERS, headers, serialized);
        return JSON.to(CancelOrdersResponse.class, response);
    }

    /**
     * Cancel all orders for a specific market or asset
     *
     * @param market  The market ID (optional)
     * @param assetId The asset ID (optional)
     * @return CancelOrdersResponse with the results
     */
    public CancelOrdersResponse cancelMarketOrders(String market, String assetId) {
        assertLevel2Auth();

        Map<String, String> body = new HashMap<>();
        body.put("market", market != null ? market : "");
        body.put("asset_id", assetId != null ? assetId : "");

        String serialized = serializeJson(body);

        RequestArgs requestArgs = RequestArgs.builder()
                .method("DELETE")
                .requestPath(CANCEL_MARKET_ORDERS)
                .body(body)
                .serializedBody(serialized)
                .build();

        Map<String, String> headers = Headers.createLevel2Headers(signer, creds, requestArgs);
        Object response = httpClient.delete(host + CANCEL_MARKET_ORDERS, headers, serialized);
        return JSON.to(CancelOrdersResponse.class, response);
    }

    // ==================== Order Helper Methods ====================

    /**
     * Convert a signed V2 order to JSON format for posting
     * Format: { "order": {...}, "orderType": "GTC", "owner": "uuid" }
     */
    private Map<String, Object> orderToJsonV2(SignedOrderV2 order, String apiKey,
                                              OrderType orderType, boolean postOnly) {
        if (order == null) {
            throw new PolyException("Order cannot be null");
        }
        if (orderType == null) {
            throw new PolyException("orderType is required and cannot be null");
        }
        if (isEmpty(order.getSignature())) {
            throw new PolyException("signature is required and cannot be empty");
        }
        if (!order.getSignature().startsWith("0x") || order.getSignature().length() < 132) {
            throw new PolyException("signature appears to be invalid (should be 0x-prefixed hex string)");
        }

        Map<String, Object> json = order.toDict();
        json.put("orderType", orderType.name());
        json.put("owner", apiKey != null ? apiKey : "");
        if (postOnly) {
            json.put("postOnly", true);
        }
        return json;
    }

    /**
     * Convert a signed V1 order to JSON format for posting
     * Format: { "order": {...}, "orderType": "GTC", "owner": "uuid" }
     */
    private Map<String, Object> orderToJson(SignedOrder order, String apiKey,
                                            OrderType orderType, boolean postOnly) {
        validateOrder(order, orderType, postOnly);

        Map<String, Object> json = order.toDict();
        json.put("orderType", orderType.name());
        json.put("owner", apiKey);
        if (postOnly) {
            json.put("postOnly", true);
        }
        return json;
    }

    private void validateOrder(SignedOrder order, OrderType orderType, boolean postOnly) {
        if (order == null) {
            throw new PolyException("Order cannot be null");
        }

        if (isEmpty(order.getTokenId())) {
            throw new PolyException("tokenId is required and cannot be empty");
        }

        if (isEmpty(order.getMaker())) {
            throw new PolyException("maker address is required and cannot be empty");
        }

        if (isEmpty(order.getSigner())) {
            throw new PolyException("signer address is required and cannot be empty");
        }

        if (isEmpty(order.getTaker())) {
            throw new PolyException("taker address is required and cannot be empty");
        }

        if (isEmpty(order.getMakerAmount())) {
            throw new PolyException("makerAmount is required and cannot be empty");
        }

        if (isEmpty(order.getTakerAmount())) {
            throw new PolyException("takerAmount is required and cannot be empty");
        }

        if (isEmpty(order.getExpiration())) {
            throw new PolyException("expiration is required and cannot be empty");
        }

        if (isEmpty(order.getNonce())) {
            throw new PolyException("nonce is required and cannot be empty");
        }

        if (isEmpty(order.getSide())) {
            throw new PolyException("side is required and cannot be empty");
        }

        if (!order.getSide().equals("BUY") && !order.getSide().equals("SELL")) {
            throw new PolyException("side must be either 'BUY' or 'SELL', got: " + order.getSide());
        }

        if (isEmpty(order.getSignature())) {
            throw new PolyException("signature is required and cannot be empty");
        }

        if (!order.getSignature().startsWith("0x") || order.getSignature().length() < 132) {
            throw new PolyException("signature appears to be invalid (should be 0x-prefixed hex string)");
        }

        if (orderType == null) {
            throw new PolyException("orderType is required and cannot be null");
        }

        if (postOnly && orderType != OrderType.GTC && orderType != OrderType.GTD) {
            throw new PolyException("postOnly orders can only be of type GTC or GTD, got: " + orderType);
        }
    }

    /**
     * Resolve tick size for a token
     */
    private String resolveTickSize(String tokenId, String tickSize) {
        String minTickSize = getTickSize(tokenId);
        if (tickSize != null) {
            if (isTickSizeSmaller(tickSize, minTickSize)) {
                throw new PolyException(String.format(
                        "Invalid tick size (%s), minimum for the market is %s",
                        tickSize, minTickSize
                ));
            }
        } else {
            tickSize = minTickSize;
        }
        return tickSize;
    }

    /**
     * Resolve fee rate for a token (V1 only)
     */
    private int resolveFeeRate(String tokenId, int userFeeRate) {
        int marketFeeRate = getFeeRateBps(tokenId);
        if (marketFeeRate > 0 && userFeeRate > 0 && userFeeRate != marketFeeRate) {
            throw new PolyException(String.format(
                    "Invalid user provided fee rate: (%d), fee rate for the market must be %d",
                    userFeeRate, marketFeeRate
            ));
        }
        return marketFeeRate;
    }

    /**
     * Check if a price is valid for the given tick size
     */
    private boolean isPriceValid(double price, String tickSize) {
        double tick = Double.parseDouble(tickSize);
        return price >= tick && price <= (1 - tick);
    }

    /**
     * Check if tick size is smaller than minimum
     */
    private boolean isTickSizeSmaller(String tickSize, String minTickSize) {
        BigDecimal tick = new BigDecimal(tickSize);
        BigDecimal minTick = new BigDecimal(minTickSize);
        return tick.compareTo(minTick) < 0;
    }

    /**
     * Calculate market price for a market order
     */
    private double calculateMarketPrice(String tokenId, String side, double amount) {
        BookEvent orderBook = getOrderBook(tokenId);

        if (Constants.BUY.equals(side)) {
            // For buys, use the ask side
            if (orderBook.getAsks() == null || orderBook.getAsks().isEmpty()) {
                throw new PolyException("No asks available in order book");
            }
            return orderBook.getAsks().get(0).getPriceAsDouble();
        } else {
            // For sells, use the bid side
            if (orderBook.getBids() == null || orderBook.getBids().isEmpty()) {
                throw new PolyException("No bids available in order book");
            }
            return orderBook.getBids().get(0).getPriceAsDouble();
        }
    }

    /**
     * Cancel an order
     *
     * @param orderId The order ID to cancel
     * @return CancelOrderResponse with the result
     */
    public CancelOrderResponse cancel(String orderId) {
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
        Object response = httpClient.delete(host + CANCEL, headers, serialized);
        return JSON.to(CancelOrderResponse.class, response);
    }

    /**
     * Cancel all orders
     *
     * @return CancelOrdersResponse with the results
     */
    public CancelOrdersResponse cancelAll() {
        assertLevel2Auth();

        RequestArgs requestArgs = RequestArgs.builder()
                .method("DELETE")
                .requestPath(CANCEL_ALL)
                .build();
        Map<String, String> headers = Headers.createLevel2Headers(signer, creds, requestArgs);
        Object response = httpClient.delete(host + CANCEL_ALL, headers);
        return JSON.to(CancelOrdersResponse.class, response);
    }

    /**
     * Get orders for the API key
     *
     * @param params The open order query parameters
     * @return List of OpenOrder objects
     */
    public List<OpenOrder> getOrders(OpenOrderParams params) {
        assertLevel2Auth();

        RequestArgs requestArgs = RequestArgs.builder()
                .method("GET")
                .requestPath(ORDERS)
                .build();
        Map<String, String> headers = Headers.createLevel2Headers(signer, creds, requestArgs);

        String url = QueryBuilder.addQueryOpenOrdersParams(host + ORDERS, params, "MA==");
        Object response = httpClient.get(url, headers);

        // Convert to list of OpenOrder
        @SuppressWarnings("unchecked")
        List<Object> responseList = (List<Object>) response;
        return responseList.stream()
                .map(obj -> JSON.to(OpenOrder.class, obj))
                .collect(Collectors.toList());
    }

    /**
     * Get an order by ID
     *
     * @param orderId The order ID to retrieve
     * @return OpenOrder object with the order details
     */
    public OpenOrder getOrder(String orderId) {
        assertLevel2Auth();

        String endpoint = GET_ORDER + orderId;
        RequestArgs requestArgs = RequestArgs.builder()
                .method("GET")
                .requestPath(endpoint)
                .build();
        Map<String, String> headers = Headers.createLevel2Headers(signer, creds, requestArgs);
        Object response = httpClient.get(host + endpoint, headers);
        return JSON.to(OpenOrder.class, response);
    }

    /**
     * Get trades for the user
     *
     * @param params The trade query parameters
     * @return TradesResponse with list of trades and pagination cursor
     */
    public TradesResponse getTrades(TradeParams params) {
        assertLevel2Auth();

        RequestArgs requestArgs = RequestArgs.builder()
                .method("GET")
                .requestPath(TRADES)
                .build();
        Map<String, String> headers = Headers.createLevel2Headers(signer, creds, requestArgs);

        String url = QueryBuilder.addQueryTradeParams(host + TRADES, params, "MA==");
        Object response = httpClient.get(url, headers);

        // Convert to TradesResponse
        return JSON.to(TradesResponse.class, response);
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

    // ==================== V2-specific Methods ====================

    /**
     * Get pre-migration (V1) orders for the current API key (Level 2 Auth required).
     *
     * @return List of pre-migration open orders
     */
    public List<OpenOrder> getPreMigrationOrders() {
        assertLevel2Auth();

        RequestArgs requestArgs = RequestArgs.builder()
                .method("GET")
                .requestPath(PRE_MIGRATION_ORDERS)
                .build();
        Map<String, String> headers = Headers.createLevel2Headers(signer, creds, requestArgs);
        Object response = httpClient.get(host + PRE_MIGRATION_ORDERS, headers);

        @SuppressWarnings("unchecked")
        List<Object> responseList = (List<Object>) response;
        return responseList.stream()
                .map(obj -> JSON.to(OpenOrder.class, obj))
                .collect(Collectors.toList());
    }

    /**
     * Post a heartbeat to keep the session alive (Level 2 Auth required).
     *
     * @param orderIds Comma-separated list of order IDs to keep alive
     * @return Server response
     */
    public Object postHeartbeat(String orderIds) {
        assertLevel2Auth();

        Map<String, String> body = orderIds != null
                ? Map.of("orderIds", orderIds)
                : Map.of();
        String serialized = serializeJson(body);

        RequestArgs requestArgs = RequestArgs.builder()
                .method("POST")
                .requestPath(POST_HEARTBEAT)
                .body(body)
                .serializedBody(serialized)
                .build();
        Map<String, String> headers = Headers.createLevel2Headers(signer, creds, requestArgs);
        return httpClient.post(host + POST_HEARTBEAT, headers, serialized);
    }

    /**
     * Get the builder fee rate for a given builder code.
     *
     * @param builderCode The builder code (bytes32 hex)
     * @return Server response containing the fee rate
     */
    public Object getBuilderFeeRate(String builderCode) {
        return httpClient.get(host + GET_BUILDER_FEE_RATE + builderCode);
    }

    /**
     * Get V2 exchange address
     */
    public String getExchangeAddressV2() {
        if (chainId == null) return null;
        return Config.getContractConfig(chainId).getExchangeV2();
    }

    /**
     * Get V2 exchange address (with neg-risk flag)
     */
    public String getExchangeAddressV2(boolean isNegRisk) {
        if (chainId == null) return null;
        ContractConfig config = Config.getContractConfig(chainId, isNegRisk);
        return isNegRisk ? config.getNegRiskExchangeV2() : config.getExchangeV2();
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
            // Sort keys alphabetically to ensure consistent ordering for HMAC signatures
            // This matches Python's json.dumps() behavior with sort_keys=True
            String json = JSON.toJSONString(obj, com.alibaba.fastjson2.JSONWriter.Feature.MapSortField);

            // Add spaces after colons and commas to match Python's default formatting
            // Python uses separators=(', ', ': ') by default
            json = json.replace(",", ", ").replace(":", ": ");

            return json;
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

    public Integer getSignatureType() {
        return signatureType;
    }

    public String getFunder() {
        return funder;
    }
}
