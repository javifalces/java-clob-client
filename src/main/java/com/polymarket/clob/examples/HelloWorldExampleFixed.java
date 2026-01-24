package com.polymarket.clob.examples;

import com.alibaba.fastjson2.JSON;
import com.polymarket.clob.ClobClient;
import com.polymarket.clob.model.*;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.polymarket.clob.Constants.*;

/**
 * Hello World Example - Comprehensive demo of Polymarket CLOB Client
 * 
 * FIXED VERSION: Now includes proper L1 & L2 authentication for order posting
 * 
 * Based on: YouTube Tutorial and Python Notebook
 * 
 * KEY FIXES:
 * - L1: EIP-712 order signature (already working)
 * - L2: HMAC-SHA256 headers for authenticated requests (NEW)
 * - API Credentials: Properly derive and use for all trading requests
 */
public class HelloWorldExampleFixed {
    private static final String GAMMA_API = "https://gamma-api.polymarket.com";
    private static final String CLOB_API = "https://clob.polymarket.com";
    
    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build();

    public static void main(String[] args) {
        try {
            // Get credentials from environment variables
            String privateKey = System.getenv("POLYMARKET_PK");
            String wallet = System.getenv("POLYMARKET_WALLET");

            System.out.println("=== Polymarket CLOB Client - Hello World Example (Fixed L2) ===\n");

            // Step 1: Fetch active markets sorted by volume
            System.out.println("Step 1: Fetching active markets sorted by volume...");
            List<Map<String, Object>> markets = fetchActiveMarkets(10);
            System.out.println("Found " + markets.size() + " markets\n");

            // Step 2: Display top 5 markets
            System.out.println("Step 2: Top 5 markets by volume");
            displayTopMarkets(markets, 5);

            // Step 3: Extract token IDs and market IDs
            System.out.println("Step 3: Extracting token IDs and market IDs...");
            List<String> allTokenIds = new ArrayList<>();
            List<String> marketIds = new ArrayList<>();

            for (Map<String, Object> market : markets) {
                String marketId = String.valueOf(market.get("id"));
                marketIds.add(marketId);
                
                String clobTokenIdsStr = String.valueOf(market.get("clobTokenIds"));
                if (clobTokenIdsStr != null && !clobTokenIdsStr.equals("null")) {
                    @SuppressWarnings("unchecked")
                    List<String> clobTokenIds = JSON.parseArray(clobTokenIdsStr, String.class);
                    allTokenIds.addAll(clobTokenIds);
                    System.out.println("Market: " + market.get("question"));
                    System.out.println("Token IDs: " + clobTokenIds);
                }
            }
            System.out.println();

            // Step 4: Get first market details
            System.out.println("Step 4: Analyzing first market...");
            Map<String, Object> market = markets.get(0);
            System.out.println("Market: " + market.get("question"));
            System.out.println("End Date: " + market.get("endDate"));
            System.out.println("Condition ID: " + market.get("conditionId"));
            System.out.println();

            // Step 5: Extract YES and NO token IDs
            String clobTokenIdsStr = String.valueOf(market.get("clobTokenIds"));
            @SuppressWarnings("unchecked")
            List<String> clobTokenIds = JSON.parseArray(clobTokenIdsStr, String.class);
            System.out.println("Token IDs: " + clobTokenIds);

            String yesTokenId = null;
            String noTokenId = null;

            if (clobTokenIds.size() >= 2) {
                yesTokenId = clobTokenIds.get(0);
                noTokenId = clobTokenIds.get(1);
                System.out.println("YES token: " + yesTokenId);
                System.out.println("NO token: " + noTokenId);
            }
            System.out.println();

            // ============================================
            // Step 6: Create CLOB client (L1 Auth)
            // ============================================
            System.out.println("Step 6: Creating CLOB client...");
            ClobClient client;
            if (privateKey != null && !privateKey.isEmpty()) {
                client = new ClobClient(CLOB_API, 137, privateKey, null, 1, wallet);
                System.out.println("✅ Created authenticated client for address: " + client.getAddress());
            } else {
                client = new ClobClient(CLOB_API);
                System.out.println("⚠️  No private key found, using public client (read-only mode)");
            }
            System.out.println();

            // ============================================
            // Step 6.5: CRITICAL - Derive API Credentials (L1 -> L2)
            // ============================================
            ApiCreds apiCreds = null;
            if (privateKey != null && !privateKey.isEmpty()) {
                System.out.println("Step 6.5: Deriving API credentials (CRITICAL for L2 authentication)...");
                try {
                    apiCreds = client.deriveApiKey();
                    client.setApiCreds(apiCreds);
                    System.out.println("✅ API credentials derived successfully!");
                    System.out.println("   API Key: " + apiCreds.getApiKey().substring(0, 8) + "...");
                    System.out.println("   Passphrase: " + apiCreds.getPassphrase().substring(0, 8) + "...");
                    System.out.println("   ⚠️  CRITICAL: These credentials MUST be included in all L2 requests");
                    System.out.println("   L2 uses HMAC-SHA256 signatures for authentication");
                } catch (Exception e) {
                    System.err.println("❌ Failed to derive API credentials: " + e.getMessage());
                    System.err.println("   Root cause analysis:");
                    System.err.println("   1. Check if wallet has sufficient balance");
                    System.err.println("   2. Verify private key is correct");
                    System.err.println("   3. Ensure signature_type matches wallet type (1 for POLY_PROXY)");
                    throw e;
                }
                System.out.println();
            }

            // Step 7: Get order book for YES token
            if (yesTokenId != null) {
                System.out.println("Step 7: Fetching YES token order book...");
                BookEvent book = client.getOrderBook(yesTokenId);
                List<OrderBookEntry> sortedBids = book.getBids().stream()
                    .sorted(Comparator.comparingDouble(OrderBookEntry::getPriceAsDouble).reversed())
                    .collect(Collectors.toList());
                List<OrderBookEntry> sortedAsks = book.getAsks().stream()
                    .sorted(Comparator.comparingDouble(OrderBookEntry::getPriceAsDouble))
                    .collect(Collectors.toList());

                System.out.println("Market: " + market.get("question"));
                System.out.println("End Date: " + market.get("endDate"));
                System.out.println("ID: " + market.get("id"));
                System.out.println("=== YES Token Order Book ===");
                printOrderBook(sortedBids, sortedAsks, 5);
            }
            System.out.println();

            // Step 8: Get order book for NO token
            if (noTokenId != null) {
                System.out.println("Step 8: Fetching NO token order book...");
                BookEvent book = client.getOrderBook(noTokenId);
                List<OrderBookEntry> sortedBids = book.getBids().stream()
                    .sorted(Comparator.comparingDouble(OrderBookEntry::getPriceAsDouble).reversed())
                    .collect(Collectors.toList());
                List<OrderBookEntry> sortedAsks = book.getAsks().stream()
                    .sorted(Comparator.comparingDouble(OrderBookEntry::getPriceAsDouble))
                    .collect(Collectors.toList());

                System.out.println("=== NO Token Order Book ===");
                printOrderBook(sortedBids, sortedAsks, 5);
            }
            System.out.println();

            // Step 9: Get market data
            if (yesTokenId != null) {
                System.out.println("Step 9: Getting market data...");
                MidpointResponse mid = client.getMidpoint(yesTokenId);
                PriceResponse price = client.getPrice(yesTokenId, BUY);
                BookEvent book = client.getOrderBook(yesTokenId);
                System.out.println("Midpoint: " + mid.getMid());
                System.out.println("Buy Price: " + price.getPrice());
                System.out.println("Market: " + book.getMarket());
                System.out.println();
            }

            // ============================================
            // Step 10: Create and POST a limit order
            // ============================================
            if (privateKey != null && noTokenId != null && apiCreds != null) {
                System.out.println("Step 10: Creating and posting limit order...");
                System.out.println("AUTHENTICATION FLOW:");
                System.out.println("  L1: Order signed with private key (EIP-712)");
                System.out.println("  L2: Request authenticated with HMAC-SHA256 using API secret");
                System.out.println();

                try {
                    // Create the order arguments
                    OrderArgs limitOrder = OrderArgs.builder()
                        .tokenId(noTokenId)
                        .size(100.0)  // in CLOB terms => 100 * 0.01 = $1.00
                        .side(BUY)
                        .price(0.01)
                        .build();

                    // Sign the order (Level 1 - requires private key)
                    System.out.println("Signing order with private key (L1)...");
                    SignedOrder signedLimitOrder = client.createOrder(limitOrder);
                    System.out.println("✅ Limit order signed!");
                    System.out.println("   Signature: " + signedLimitOrder.getSignature().substring(0, 20) + "...");
                    System.out.println("   Order ID: " + signedLimitOrder.getId());

                    // POST the order to the exchange (Level 2 - requires API credentials + HMAC)
                    System.out.println("\nPosting order to exchange (L2)...");
                    System.out.println("   Using API credentials for HMAC-SHA256 authentication");
                    System.out.println("   Headers will include:");
                    System.out.println("     - POLY_ADDRESS: " + client.getAddress());
                    System.out.println("     - POLY_API_KEY: " + apiCreds.getApiKey().substring(0, 8) + "...");
                    System.out.println("     - POLY_SIGNATURE: HMAC-SHA256(timestamp+method+path+body)");
                    System.out.println("     - POLY_TIMESTAMP: server timestamp");
                    System.out.println();

                    OrderResponse response = client.postOrder(signedLimitOrder);
                    System.out.println("✅ Limit order posted successfully!");
                    System.out.println("   Order ID: " + response.getOrderId());
                    System.out.println("   Status: " + response.getStatus());
                    System.out.println();

                    // Cancel the order
                    System.out.println("Cancelling order...");
                    CancelOrderResponse cancelResponse = client.cancel(response.getOrderId());
                    System.out.println("✅ Order cancelled!");
                    System.out.println("   Cancelled order ID: " + cancelResponse.getOrderId());

                } catch (Exception e) {
                    System.err.println("❌ Error posting order: " + e.getMessage());
                    System.err.println("\n📋 TROUBLESHOOTING GUIDE:");
                    
                    if (e.getMessage().contains("400")) {
                        System.err.println("\n1. HTTP 400 - Invalid Signature:");
                        System.err.println("   • Order L1 signature (EIP-712) is invalid");
                        System.err.println("   • Check: Private key format (0x hex string)");
                        System.err.println("   • Check: Signature type matches wallet (1=POLY_PROXY)");
                        System.err.println("   • Check: Wallet/funder address is correct");
                        System.err.println("   • Verify: Order fields match market requirements");
                    }
                    
                    if (e.getMessage().contains("401")) {
                        System.err.println("\n2. HTTP 401 - Unauthorized (L2 Auth Failed):");
                        System.err.println("   • HMAC-SHA256 signature validation failed");
                        System.err.println("   • Check: API credentials were derived (step 6.5)");
                        System.err.println("   • Check: POLY_API_KEY header is being sent");
                        System.err.println("   • Check: HMAC message = timestamp + method + path + body");
                        System.err.println("   • Check: HMAC uses correct API secret");
                        System.err.println("   • Check: Timestamp is within ±30 seconds of server time");
                        System.err.println("   • Note: Python uses nanoseconds, verify conversion");
                    }
                    
                    System.err.println("\n3. General L2 Auth Issues:");
                    System.err.println("   • Ensure client.setApiCreds() was called with valid credentials");
                    System.err.println("   • Verify postOrder() uses L2 headers (POLY_API_KEY, POLY_SIGNATURE)");
                    System.err.println("   • Check that ClobClient.postOrder includes HMAC generation");
                    
                    e.printStackTrace();
                }
            }

            System.out.println("\n✅ Example completed successfully!");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Fetch active markets from Gamma API
     */
    private static List<Map<String, Object>> fetchActiveMarkets(int limit) throws IOException {
        String url = String.format("%s/markets?limit=%d&active=true&closed=false&order=volume24hr&ascending=false",
                GAMMA_API, limit);
        
        Request request = new Request.Builder()
            .url(url)
            .get()
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code: " + response);
            }
            
            if (response.body() == null) {
                throw new IOException("Response body is null");
            }
            
            String body = response.body().string();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> markets = JSON.parseArray(body, Map.class);
            return markets;
        }
    }

    /**
     * Display top markets with their details
     */
    private static void displayTopMarkets(List<Map<String, Object>> markets, int count) {
        for (int i = 0; i < Math.min(count, markets.size()); i++) {
            Map<String, Object> m = markets.get(i);
            System.out.println("Question: " + m.get("question"));
            System.out.println("  Volume 24h: $" + formatNumber(m.get("volume24hr")));
            System.out.println("  Liquidity: $" + formatNumber(m.get("liquidityNum")));
            System.out.println("  Prices: " + m.get("outcomePrices"));
            System.out.println();
        }
    }

    /**
     * Print order book in a formatted table
     */
    private static void printOrderBook(List<OrderBookEntry> bids, List<OrderBookEntry> asks, int depth) {
        System.out.printf("%-40s | %s%n", "BIDS (Buy Orders)", "ASKS (Sell Orders)");
        System.out.printf("%-20s %-20s | %-20s %-20s%n", "Size", "Price", "Price", "Size");
        System.out.println("-".repeat(85));

        for (int i = 0; i < depth; i++) {
            OrderBookEntry bid = i < bids.size() ? bids.get(i) : null;
            OrderBookEntry ask = i < asks.size() ? asks.get(i) : null;

            String bidStr = bid != null ? String.format("%-20s %-20s", bid.getSize(), bid.getPrice()) : " ".repeat(40);
            String askStr = ask != null ? String.format("%-20s %-20s", ask.getPrice(), ask.getSize()) : " ".repeat(40);

            System.out.printf("%s | %s%n", bidStr, askStr);
        }
        System.out.println();
    }

    /**
     * Format number for display
     */
    private static String formatNumber(Object value) {
        if (value == null) return "0";
        if (value instanceof Number) {
            return String.format("%,.0f", ((Number) value).doubleValue());
        }
        return value.toString();
    }
}
