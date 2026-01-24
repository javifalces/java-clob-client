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
 * <p>
 * Based on: <a href="https://www.youtube.com/watch?v=dTyY6rft5kg">YouTube Tutorial</a>
 * and <a href="https://github.com/RobotTraders/bits_and_bobs/blob/main/polymarket_python.ipynb">Python Notebook</a>
 */
public class HelloWorldExample {

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

            System.out.println("=== Polymarket CLOB Client - Hello World Example ===\n");

            // Step 1: Fetch active markets sorted by volume
            System.out.println("Step 1: Fetching active markets sorted by volume...");
            List<Map<String, Object>> markets = fetchActiveMarkets(10);
            System.out.println("Found " + markets.size() + " markets\n");

            // Step 2: Display top 5 markets
            System.out.println("Step 2: Top 5 markets by volume:");
            displayTopMarkets(markets, 5);

            // Step 3: Extract token IDs and market IDs
            System.out.println("\nStep 3: Extracting token IDs and market IDs...");
            List<String> allTokenIds = new ArrayList<>();
            List<String> marketIds = new ArrayList<>();

            for (Map<String, Object> market : markets) {
                String marketId = (String) market.get("id");
                marketIds.add(marketId);

                String clobTokenIdsStr = (String) market.get("clobTokenIds");
                if (clobTokenIdsStr != null) {
                    @SuppressWarnings("unchecked")
                    List<String> clobTokenIds = JSON.parseArray(clobTokenIdsStr, String.class);
                    allTokenIds.addAll(clobTokenIds);
                    System.out.println("Market: " + market.get("question"));
                    System.out.println("Token IDs: " + clobTokenIds);
                    System.out.println();
                }
            }

            System.out.println("All Token IDs:");
            System.out.println(String.join(", ", allTokenIds));
            System.out.println("\nAll Market IDs:");
            System.out.println(String.join(", ", marketIds));

            // Step 4: Get first market details
            System.out.println("\n\nStep 4: Analyzing first market...");
            Map<String, Object> market = markets.get(0);
            System.out.println("Market: " + market.get("question"));
            System.out.println("End Date: " + market.get("endDate"));
            System.out.println("Condition ID: " + market.get("conditionId"));

            // Step 5: Extract YES and NO token IDs
            String clobTokenIdsStr = (String) market.get("clobTokenIds");
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

            // Step 6: Create CLOB client
            System.out.println("\n\nStep 6: Creating CLOB client...");
            ClobClient client;
            if (privateKey != null && !privateKey.isEmpty()) {
                client = new ClobClient(CLOB_API, 137, privateKey, null, 1, wallet);
                System.out.println("Created authenticated client for address: " + client.getAddress());

                // Derive API key (optional - only if you want full L2 access)
                // Uncomment the following lines to create API credentials
                ApiCreds apiCreds = client.deriveApiKey();
                client.setApiCreds(apiCreds);


                System.out.println("API credentials created and set.");
            } else {
                client = new ClobClient(CLOB_API);
                System.out.println("No API key found, using public client.");
            }

            // Step 7: Get order book for YES token
            if (yesTokenId != null) {
                System.out.println("\n\nStep 7: Fetching YES token order book...");
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

            // Step 8: Get order book for NO token
            if (noTokenId != null) {
                System.out.println("\n\nStep 8: Fetching NO token order book...");
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

            // Step 9: Get market data
            if (yesTokenId != null) {
                System.out.println("\n\nStep 9: Getting market data...");
                MidpointResponse mid = client.getMidpoint(yesTokenId);
                PriceResponse price = client.getPrice(yesTokenId, BUY);
                BookEvent book = client.getOrderBook(yesTokenId);

                System.out.println("Midpoint: " + mid.getMid());
                System.out.println("Buy Price: " + price.getPrice());
                System.out.println("Market: " + book.getMarket());
            }

            // Step 10: Create and post a limit order (requires L2 authentication)
            if (privateKey != null && noTokenId != null) {
                System.out.println("\n\nStep 10: Creating limit order example...");
                System.out.println("NOTE: To actually post orders, you need to:");
                System.out.println("1. Create API credentials (uncomment the createApiKey section above)");
                System.out.println("2. Set the credentials on the client");
                System.out.println("3. Uncomment the order posting code below");

                System.out.println("\nExample order creation:");
                OrderArgs limitOrder = OrderArgs.builder()
                        .tokenId(noTokenId)
                        .size(100.0)  // in clob terms => 100*0.01 = $1.00
                        .side(BUY)
                        .price(0.01)
                        .build();

                SignedOrder signedLimitOrder = client.createOrder(limitOrder);
                System.out.println("Limit order signed!");
                System.out.println("Order: " + signedLimitOrder);

                // To actually post the order (requires L2 auth with API credentials):
                OrderResponse response = client.postOrder(signedLimitOrder);
                System.out.println("Limit order executed!");
                System.out.println("Response: " + response);

                // To cancel the order:
                client.cancelOrders(List.of(response.getOrderId()));
                System.out.println("Order cancelled!");
            }

            System.out.println("\n\n=== Example completed successfully! ===");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
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
                throw new IOException("Unexpected code " + response);
            }

            if (response.body() == null) {
                throw new IOException("Response body is null");
            }

            String body = response.body().string();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> markets = (List<Map<String, Object>>) JSON.parse(body);
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
        System.out.printf("\n%-40s | %s\n", "BIDS (Buy Orders)", "ASKS (Sell Orders)");
        System.out.printf("%-20s %-20s | %-20s %-20s\n", "Size", "Price", "Price", "Size");
        System.out.println("-".repeat(85));

        for (int i = 0; i < depth; i++) {
            OrderBookEntry bid = i < bids.size() ? bids.get(i) : null;
            OrderBookEntry ask = i < asks.size() ? asks.get(i) : null;

            String bidStr = bid != null
                    ? String.format("%-20s %-20s", bid.getSize(), bid.getPrice())
                    : " ".repeat(40);
            String askStr = ask != null
                    ? String.format("%-20s %-20s", ask.getPrice(), ask.getSize())
                    : " ".repeat(40);

            System.out.printf("%s | %s\n", bidStr, askStr);
        }
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
