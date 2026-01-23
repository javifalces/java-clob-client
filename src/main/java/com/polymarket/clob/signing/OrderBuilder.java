package com.polymarket.clob.signing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.polymarket.clob.Constants;
import com.polymarket.clob.config.Config;
import com.polymarket.clob.model.*;
import org.web3j.crypto.StructuredDataEncoder;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;

/**
 * Builds and signs orders for the CLOB
 * Migrated from Python implementation with proper rounding logic
 */
public class OrderBuilder {

    public enum SignatureType {
        EOA(0),
        POLY_PROXY(1),
        POLY_GNOSIS_SAFE(2);

        private final int value;

        SignatureType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final SecureRandom random = new SecureRandom();

    // Rounding configuration based on tick size (matching Python ROUNDING_CONFIG)
    private static final Map<String, RoundConfig> ROUNDING_CONFIG = new HashMap<>();

    static {
        ROUNDING_CONFIG.put("0.1", new RoundConfig(1, 2, 3));
        ROUNDING_CONFIG.put("0.01", new RoundConfig(2, 2, 4));
        ROUNDING_CONFIG.put("0.001", new RoundConfig(3, 2, 5));
        ROUNDING_CONFIG.put("0.0001", new RoundConfig(4, 2, 6));
    }

    private final Signer signer;
    private final int signatureType;
    private final String funder;

    /**
     * Create a new OrderBuilder
     *
     * @param signer        The signer for authentication
     * @param signatureType The signature type (0 for EOA, 1 for Poly Proxy, 2 for Poly Gnosis Safe)
     * @param funder        The funder address (optional, defaults to signer address)
     */
    public OrderBuilder(Signer signer, int signatureType, String funder) {
        this.signer = signer;
        this.signatureType = signatureType;
        this.funder = funder != null ? funder : signer.getAddress();
    }

    /**
     * Create a new OrderBuilder with default signature type (Poly proxy)
     */
    public OrderBuilder(Signer signer) {
        this(signer, 1, null);
    }

    /**
     * Create and sign an order
     *
     * @param orderArgs The order arguments
     * @param options   The creation options
     * @return A signed order ready to post
     */
    public SignedOrder createOrder(OrderArgs orderArgs, CreateOrderOptions options) {
        // Generate salt
        BigInteger salt = generateSalt();

        // Get rounding config for tick size
        RoundConfig roundConfig = ROUNDING_CONFIG.get(options.getTickSize());
        if (roundConfig == null) {
            throw new IllegalArgumentException("Invalid tick size: " + options.getTickSize());
        }

        // Calculate amounts using Python's logic
        OrderAmounts amounts = getOrderAmounts(
                orderArgs.getSide(),
                orderArgs.getSize(),
                orderArgs.getPrice(),
                roundConfig
        );

        // Build the order
        Order order = Order.builder()
                .salt(salt)
                .maker(normalizeAddress(funder))
                .signer(normalizeAddress(signer.getAddress()))
                .taker(normalizeAddress(orderArgs.getTaker()))
                .tokenId(orderArgs.getTokenId())
                .makerAmount(String.valueOf(amounts.getMakerAmount()))
                .takerAmount(String.valueOf(amounts.getTakerAmount()))
                .expiration(String.valueOf(orderArgs.getExpiration()))
                .nonce(String.valueOf(orderArgs.getNonce()))
                .feeRateBps(String.valueOf(orderArgs.getFeeRateBps()))
                .side(Constants.BUY.equals(orderArgs.getSide()) ? 0 : 1)
                .signatureType(signatureType)
                .build();

        // Sign the order
        String signature = signOrder(order, options.isNegRisk());

        // Return signed order
        return SignedOrder.builder()
                .order(order)
                .signature(signature)
                .build();
    }

    /**
     * Create and sign a market order
     *
     * @param orderArgs The market order arguments
     * @param options   The creation options
     * @return A signed order ready to post
     */
    public SignedOrder createMarketOrder(MarketOrderArgs orderArgs, CreateOrderOptions options) {
        // Generate salt
        BigInteger salt = generateSalt();

        // Get rounding config for tick size
        RoundConfig roundConfig = ROUNDING_CONFIG.get(options.getTickSize());
        if (roundConfig == null) {
            throw new IllegalArgumentException("Invalid tick size: " + options.getTickSize());
        }

        // Calculate amounts using Python's market order logic
        OrderAmounts amounts = getMarketOrderAmounts(
                orderArgs.getSide(),
                orderArgs.getAmount(),
                orderArgs.getPrice(),
                roundConfig
        );

        // Build the order
        Order order = Order.builder()
                .salt(salt)
                .maker(normalizeAddress(funder))
                .signer(normalizeAddress(signer.getAddress()))
                .taker(normalizeAddress(orderArgs.getTaker() != null ? orderArgs.getTaker() : Constants.ZERO_ADDRESS))
                .tokenId(orderArgs.getTokenId())
                .makerAmount(String.valueOf(amounts.getMakerAmount()))
                .takerAmount(String.valueOf(amounts.getTakerAmount()))
                .expiration("0")  // Market orders don't have expiration
                .nonce(String.valueOf(orderArgs.getNonce()))
                .feeRateBps(String.valueOf(orderArgs.getFeeRateBps()))
                .side(Constants.BUY.equals(orderArgs.getSide()) ? 0 : 1)
                .signatureType(signatureType)
                .build();

        // Sign the order
        String signature = signOrder(order, options.isNegRisk());

        // Return signed order
        return SignedOrder.builder()
                .order(order)
                .signature(signature)
                .build();
    }

    /**
     * Build a signed order from OrderData
     * Matches Python's UtilsOrderBuilder.build_signed_order() method
     *
     * @param orderData The order data
     * @param negRisk   Whether this is a negative risk market
     * @return A signed order ready to post
     */
    public SignedOrder buildSignedOrder(OrderData orderData, boolean negRisk) {
        // Generate salt if not provided
        BigInteger salt = orderData.getSalt() != null ? orderData.getSalt() : generateSalt();

        // Determine signer (use maker if signer not specified)
        String signerAddr = orderData.getSigner() != null ? orderData.getSigner() : orderData.getMaker();

        // Build the order
        Order order = Order.builder()
                .salt(salt)
                .maker(normalizeAddress(orderData.getMaker()))
                .signer(normalizeAddress(signerAddr))
                .taker(normalizeAddress(orderData.getTaker()))
                .tokenId(orderData.getTokenId())
                .makerAmount(orderData.getMakerAmount())
                .takerAmount(orderData.getTakerAmount())
                .expiration(orderData.getExpiration())
                .nonce(orderData.getNonce())
                .feeRateBps(orderData.getFeeRateBps())
                .side(orderData.getSide())
                .signatureType(orderData.getSignatureType() != null ? orderData.getSignatureType() : signatureType)
                .build();

        // Sign the order
        String signature = signOrder(order, negRisk);

        // Return signed order
        return SignedOrder.builder()
                .order(order)
                .signature(signature)
                .build();
    }

    /**
     * Get order amounts for a standard order (matches Python get_order_amounts)
     */
    private OrderAmounts getOrderAmounts(String side, double size, double price, RoundConfig roundConfig) {
        double rawPrice = roundNormal(price, roundConfig.getPrice());

        if (Constants.BUY.equals(side)) {
            // For BUY orders: maker pays price*size, receives size
            double rawTakerAmt = roundDown(size, roundConfig.getSize());

            double rawMakerAmt = rawTakerAmt * rawPrice;
            if (decimalPlaces(rawMakerAmt) > roundConfig.getAmount()) {
                rawMakerAmt = roundUp(rawMakerAmt, roundConfig.getAmount() + 4);
                if (decimalPlaces(rawMakerAmt) > roundConfig.getAmount()) {
                    rawMakerAmt = roundDown(rawMakerAmt, roundConfig.getAmount());
                }
            }

            long makerAmount = toTokenDecimals(rawMakerAmt);
            long takerAmount = toTokenDecimals(rawTakerAmt);

            return new OrderAmounts(makerAmount, takerAmount);

        } else if (Constants.SELL.equals(side)) {
            // For SELL orders: maker pays size, receives price*size
            double rawMakerAmt = roundDown(size, roundConfig.getSize());

            double rawTakerAmt = rawMakerAmt * rawPrice;
            if (decimalPlaces(rawTakerAmt) > roundConfig.getAmount()) {
                rawTakerAmt = roundUp(rawTakerAmt, roundConfig.getAmount() + 4);
                if (decimalPlaces(rawTakerAmt) > roundConfig.getAmount()) {
                    rawTakerAmt = roundDown(rawTakerAmt, roundConfig.getAmount());
                }
            }

            long makerAmount = toTokenDecimals(rawMakerAmt);
            long takerAmount = toTokenDecimals(rawTakerAmt);

            return new OrderAmounts(makerAmount, takerAmount);

        } else {
            throw new IllegalArgumentException("order_args.side must be 'BUY' or 'SELL'");
        }
    }

    /**
     * Get order amounts for a market order (matches Python get_market_order_amounts)
     */
    private OrderAmounts getMarketOrderAmounts(String side, double amount, double price, RoundConfig roundConfig) {
        double rawPrice = roundNormal(price, roundConfig.getPrice());

        if (Constants.BUY.equals(side)) {
            // BUY orders: amount is in $$$, need to calculate shares
            double rawMakerAmt = roundDown(amount, roundConfig.getSize());
            double rawTakerAmt = rawMakerAmt / rawPrice;

            if (decimalPlaces(rawTakerAmt) > roundConfig.getAmount()) {
                rawTakerAmt = roundUp(rawTakerAmt, roundConfig.getAmount() + 4);
                if (decimalPlaces(rawTakerAmt) > roundConfig.getAmount()) {
                    rawTakerAmt = roundDown(rawTakerAmt, roundConfig.getAmount());
                }
            }

            long makerAmount = toTokenDecimals(rawMakerAmt);
            long takerAmount = toTokenDecimals(rawTakerAmt);

            return new OrderAmounts(makerAmount, takerAmount);

        } else if (Constants.SELL.equals(side)) {
            // SELL orders: amount is in shares
            double rawMakerAmt = roundDown(amount, roundConfig.getSize());
            double rawTakerAmt = rawMakerAmt * rawPrice;

            if (decimalPlaces(rawTakerAmt) > roundConfig.getAmount()) {
                rawTakerAmt = roundUp(rawTakerAmt, roundConfig.getAmount() + 4);
                if (decimalPlaces(rawTakerAmt) > roundConfig.getAmount()) {
                    rawTakerAmt = roundDown(rawTakerAmt, roundConfig.getAmount());
                }
            }

            long makerAmount = toTokenDecimals(rawMakerAmt);
            long takerAmount = toTokenDecimals(rawTakerAmt);

            return new OrderAmounts(makerAmount, takerAmount);

        } else {
            throw new IllegalArgumentException("order_args.side must be 'BUY' or 'SELL'");
        }
    }

    /**
     * Sign an order using EIP-712
     *
     * @param order   The order to sign
     * @param negRisk Whether this is a negative risk market
     * @return The signature as a hex string
     */
    private String signOrder(Order order, boolean negRisk) {
        try {
            // Get the contract config for the exchange address
            ContractConfig config = Config.getContractConfig(signer.getChainId(), negRisk);
            String exchangeAddress = config.getExchange();

            // Build EIP-712 typed data
            Map<String, Object> typedData = new HashMap<>();

            // Domain
            Map<String, Object> domain = new HashMap<>();
            domain.put("name", "Polymarket CTF Exchange");
            domain.put("version", "1");
            domain.put("chainId", signer.getChainId());
            domain.put("verifyingContract", exchangeAddress);
            typedData.put("domain", domain);

            // Types
            Map<String, List<Map<String, String>>> types = new HashMap<>();

            // Define EIP712Domain type (required by StructuredDataEncoder)
            List<Map<String, String>> domainType = Arrays.asList(
                    createType("name", "string"),
                    createType("version", "string"),
                    createType("chainId", "uint256"),
                    createType("verifyingContract", "address")
            );
            types.put("EIP712Domain", domainType);

            // Define Order type
            List<Map<String, String>> orderType = Arrays.asList(
                    createType("salt", "uint256"),
                    createType("maker", "string"),
                    createType("signer", "string"),
                    createType("taker", "string"),
                    createType("tokenId", "uint256"),
                    createType("makerAmount", "uint256"),
                    createType("takerAmount", "uint256"),
                    createType("expiration", "uint256"),
                    createType("nonce", "uint256"),
                    createType("feeRateBps", "uint256"),
                    createType("side", "uint8"),
                    createType("signatureType", "uint8")
            );
            types.put("Order", orderType);
            typedData.put("types", types);

            // Message - amounts are already in base units (from getOrderAmounts)
            // NOTE: StructuredDataEncoder expects addresses WITHOUT 0x prefix
            Map<String, Object> message = new HashMap<>();
            message.put("salt", order.getSalt());
            message.put("maker", stripHexPrefix(order.getMaker()));
            message.put("signer", stripHexPrefix(order.getSigner()));
            message.put("taker", stripHexPrefix(order.getTaker()));
            message.put("tokenId", order.getTokenId());
            message.put("makerAmount", order.getMakerAmount());
            message.put("takerAmount", order.getTakerAmount());
            message.put("expiration", order.getExpiration());
            message.put("nonce", order.getNonce());
            message.put("feeRateBps", order.getFeeRateBps());
            message.put("side", order.getSide());
            message.put("signatureType", order.getSignatureType());
            typedData.put("message", message);

            // Primary type
            typedData.put("primaryType", "Order");

            // Convert to JSON and create structured data encoder
            String jsonTypedData = objectMapper.writeValueAsString(typedData);
            StructuredDataEncoder encoder = new StructuredDataEncoder(jsonTypedData);

            // Get the hash to sign
            byte[] structHash = encoder.hashStructuredData();

            // Sign and return
            String signature = signer.sign(structHash);
            return signature.startsWith("0x") ? signature : "0x" + signature;

        } catch (IOException e) {
            throw new RuntimeException("Failed to sign order", e);
        }
    }

    /**
     * Generate a random salt value
     */
    private BigInteger generateSalt() {
        byte[] bytes = new byte[7];
        random.nextBytes(bytes);
        BigInteger salt = new BigInteger(1, bytes);
        return salt;
    }

    /**
     * Create a type definition for EIP-712
     */
    private static Map<String, String> createType(String name, String type) {
        Map<String, String> typeMap = new HashMap<>();
        typeMap.put("name", name);
        typeMap.put("type", type);
        return typeMap;
    }

    // ========== Rounding Helper Methods (matching Python helpers.py) ==========

    /**
     * Round down to specified number of significant digits (matches Python round_down)
     */
    private static double roundDown(double x, int sigDigits) {
        double multiplier = Math.pow(10, sigDigits);
        return Math.floor(x * multiplier) / multiplier;
    }

    /**
     * Round normally to specified number of significant digits (matches Python round_normal)
     */
    private static double roundNormal(double x, int sigDigits) {
        double multiplier = Math.pow(10, sigDigits);
        return Math.round(x * multiplier) / multiplier;
    }

    /**
     * Round up to specified number of significant digits (matches Python round_up)
     */
    private static double roundUp(double x, int sigDigits) {
        double multiplier = Math.pow(10, sigDigits);
        return Math.ceil(x * multiplier) / multiplier;
    }

    /**
     * Convert to token decimals (matches Python to_token_decimals)
     * Multiplies by 10^6 for USDC
     */
    private static long toTokenDecimals(double x) {
        double f = 1000000.0 * x;  // 10^6 for USDC
        if (decimalPlaces(f) > 0) {
            f = roundNormal(f, 0);
        }
        return (long) f;
    }

    /**
     * Get number of decimal places (matches Python decimal_places)
     */
    private static int decimalPlaces(double x) {
        BigDecimal bd = BigDecimal.valueOf(x);
        return Math.max(0, bd.stripTrailingZeros().scale());
    }

    /**
     * Normalize an Ethereum address to ensure it has the 0x prefix
     * Required for EIP-712 encoding
     *
     * @param address The address to normalize (can be null)
     * @return The address with 0x prefix, or null if input was null
     */
    private static String normalizeAddress(String address) {
        if (address == null) {
            return null;
        }
        return address.startsWith("0x") ? address : "0x" + address;
    }

    /**
     * Strip the 0x prefix from a hex string (for EIP-712 encoding)
     *
     * @param hexString The hex string (can be null)
     * @return The hex string without 0x prefix, or null if input was null
     */
    private static String stripHexPrefix(String hexString) {
        if (hexString == null) {
            return null;
        }
        return hexString.startsWith("0x") ? hexString.substring(2) : hexString;
    }
}

