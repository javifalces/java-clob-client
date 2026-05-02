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
 * Builds and signs orders for the CLOB.
 * Supports both V1 (legacy) and V2 order formats.
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

    // ==================== V2 Order Creation ====================

    /**
     * Create and sign a V2 limit order.
     * <p>
     * V2 orders use the new exchange contract and EIP712 domain version "2".
     * The struct includes timestamp, metadata, and builder fields.
     *
     * @param orderArgs The order arguments
     * @param options   The creation options
     * @return A signed V2 order ready to post
     */
    public SignedOrderV2 createOrderV2(OrderArgs orderArgs, CreateOrderOptions options) {
        BigInteger salt = generateSalt();

        RoundConfig roundConfig = ROUNDING_CONFIG.get(options.getTickSize());
        if (roundConfig == null) {
            throw new IllegalArgumentException("Invalid tick size: " + options.getTickSize());
        }

        OrderAmounts amounts = getOrderAmounts(
                orderArgs.getSide(),
                orderArgs.getSize(),
                orderArgs.getPrice(),
                roundConfig
        );

        String timestamp = String.valueOf(System.currentTimeMillis());
        String metadata = orderArgs.getMetadata() != null ? orderArgs.getMetadata() : Constants.BYTES32_ZERO;
        String builderCode = orderArgs.getBuilderCode() != null ? orderArgs.getBuilderCode() : Constants.BYTES32_ZERO;

        OrderV2 order = OrderV2.builder()
                .salt(salt)
                .maker(normalizeAddress(funder))
                .signer(normalizeAddress(signer.getAddress()))
                .tokenId(orderArgs.getTokenId())
                .makerAmount(String.valueOf(amounts.getMakerAmount()))
                .takerAmount(String.valueOf(amounts.getTakerAmount()))
                .side(Constants.BUY.equals(orderArgs.getSide()) ? 0 : 1)
                .signatureType(signatureType)
                .timestamp(timestamp)
                .metadata(metadata)
                .builder(builderCode)
                .expiration(String.valueOf(orderArgs.getExpiration()))
                .build();

        String signature = signOrderV2(order, options.isNegRisk());

        return SignedOrderV2.builder()
                .order(order)
                .signature(signature)
                .build();
    }

    /**
     * Create and sign a V2 market order.
     * <p>
     * V2 market orders use round_down for price (instead of round_normal in V1).
     *
     * @param orderArgs The market order arguments
     * @param options   The creation options
     * @return A signed V2 order ready to post
     */
    public SignedOrderV2 createMarketOrderV2(MarketOrderArgs orderArgs, CreateOrderOptions options) {
        BigInteger salt = generateSalt();

        RoundConfig roundConfig = ROUNDING_CONFIG.get(options.getTickSize());
        if (roundConfig == null) {
            throw new IllegalArgumentException("Invalid tick size: " + options.getTickSize());
        }

        // V2 change: market orders use roundDown for price (V1 used roundNormal)
        OrderAmounts amounts = getMarketOrderAmountsV2(
                orderArgs.getSide(),
                orderArgs.getAmount(),
                orderArgs.getPrice(),
                roundConfig
        );

        String timestamp = String.valueOf(System.currentTimeMillis());
        String metadata = orderArgs.getMetadata() != null ? orderArgs.getMetadata() : Constants.BYTES32_ZERO;
        String builderCode = orderArgs.getBuilderCode() != null ? orderArgs.getBuilderCode() : Constants.BYTES32_ZERO;

        OrderV2 order = OrderV2.builder()
                .salt(salt)
                .maker(normalizeAddress(funder))
                .signer(normalizeAddress(signer.getAddress()))
                .tokenId(orderArgs.getTokenId())
                .makerAmount(String.valueOf(amounts.getMakerAmount()))
                .takerAmount(String.valueOf(amounts.getTakerAmount()))
                .side(Constants.BUY.equals(orderArgs.getSide()) ? 0 : 1)
                .signatureType(signatureType)
                .timestamp(timestamp)
                .metadata(metadata)
                .builder(builderCode)
                .expiration("0")
                .build();

        String signature = signOrderV2(order, options.isNegRisk());

        return SignedOrderV2.builder()
                .order(order)
                .signature(signature)
                .build();
    }

    // ==================== V1 Order Creation (legacy) ====================

    /**
     * Create and sign a V1 (legacy) order.
     *
     * @param orderArgs The order arguments
     * @param options   The creation options
     * @return A signed V1 order ready to post
     */
    public SignedOrder createOrder(OrderArgs orderArgs, CreateOrderOptions options) {
        BigInteger salt = generateSalt();

        RoundConfig roundConfig = ROUNDING_CONFIG.get(options.getTickSize());
        if (roundConfig == null) {
            throw new IllegalArgumentException("Invalid tick size: " + options.getTickSize());
        }

        OrderAmounts amounts = getOrderAmounts(
                orderArgs.getSide(),
                orderArgs.getSize(),
                orderArgs.getPrice(),
                roundConfig
        );

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

        String signature = signOrder(order, options.isNegRisk());

        return SignedOrder.builder()
                .order(order)
                .signature(signature)
                .build();
    }

    /**
     * Create and sign a V1 (legacy) market order.
     *
     * @param orderArgs The market order arguments
     * @param options   The creation options
     * @return A signed V1 order ready to post
     */
    public SignedOrder createMarketOrder(MarketOrderArgs orderArgs, CreateOrderOptions options) {
        BigInteger salt = generateSalt();

        RoundConfig roundConfig = ROUNDING_CONFIG.get(options.getTickSize());
        if (roundConfig == null) {
            throw new IllegalArgumentException("Invalid tick size: " + options.getTickSize());
        }

        OrderAmounts amounts = getMarketOrderAmounts(
                orderArgs.getSide(),
                orderArgs.getAmount(),
                orderArgs.getPrice(),
                roundConfig
        );

        Order order = Order.builder()
                .salt(salt)
                .maker(normalizeAddress(funder))
                .signer(normalizeAddress(signer.getAddress()))
                .taker(normalizeAddress(orderArgs.getTaker() != null ? orderArgs.getTaker() : Constants.ZERO_ADDRESS))
                .tokenId(orderArgs.getTokenId())
                .makerAmount(String.valueOf(amounts.getMakerAmount()))
                .takerAmount(String.valueOf(amounts.getTakerAmount()))
                .expiration("0")
                .nonce(String.valueOf(orderArgs.getNonce()))
                .feeRateBps(String.valueOf(orderArgs.getFeeRateBps()))
                .side(Constants.BUY.equals(orderArgs.getSide()) ? 0 : 1)
                .signatureType(signatureType)
                .build();

        String signature = signOrder(order, options.isNegRisk());

        return SignedOrder.builder()
                .order(order)
                .signature(signature)
                .build();
    }

    /**
     * Build a signed V1 order from OrderData
     *
     * @param orderData The order data
     * @param negRisk   Whether this is a negative risk market
     * @return A signed V1 order ready to post
     */
    public SignedOrder buildSignedOrder(OrderData orderData, boolean negRisk) {
        BigInteger salt = orderData.getSalt() != null ? orderData.getSalt() : generateSalt();

        String signerAddr = orderData.getSigner() != null ? orderData.getSigner() : orderData.getMaker();

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

        String signature = signOrder(order, negRisk);

        return SignedOrder.builder()
                .order(order)
                .signature(signature)
                .build();
    }

    // ==================== Order Amount Calculations ====================

    /**
     * Get order amounts for a standard limit order (matches Python get_order_amounts)
     */
    private OrderAmounts getOrderAmounts(String side, double size, double price, RoundConfig roundConfig) {
        double rawPrice = roundNormal(price, roundConfig.getPrice());

        if (Constants.BUY.equals(side)) {
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
     * Get order amounts for a V1 market order (uses roundNormal for price)
     */
    private OrderAmounts getMarketOrderAmounts(String side, double amount, double price, RoundConfig roundConfig) {
        double rawPrice = roundNormal(price, roundConfig.getPrice());

        if (Constants.BUY.equals(side)) {
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
     * Get order amounts for a V2 market order (uses roundDown for price - V2 change)
     */
    private OrderAmounts getMarketOrderAmountsV2(String side, double amount, double price, RoundConfig roundConfig) {
        // V2 change: use roundDown instead of roundNormal for market order price
        double rawPrice = roundDown(price, roundConfig.getPrice());

        if (Constants.BUY.equals(side)) {
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

    // ==================== EIP712 Signing ====================

    /**
     * Sign a V2 order using EIP-712 with the V2 exchange contract.
     * <p>
     * V2 struct fields: salt, maker, signer, tokenId, makerAmount, takerAmount,
     * side, signatureType, timestamp, metadata (bytes32), builder (bytes32).
     * Note: expiration is NOT part of the EIP712 hash in V2.
     *
     * @param order   The V2 order to sign
     * @param negRisk Whether this is a negative risk market
     * @return The signature as a hex string
     */
    private String signOrderV2(OrderV2 order, boolean negRisk) {
        try {
            ContractConfig config = Config.getContractConfig(signer.getChainId(), negRisk);
            String exchangeAddress = negRisk ? config.getNegRiskExchangeV2() : config.getExchangeV2();

            Map<String, Object> typedData = new HashMap<>();

            // Domain - version "2" for V2 orders
            Map<String, Object> domain = new HashMap<>();
            domain.put("name", "Polymarket CTF Exchange");
            domain.put("version", "2");
            domain.put("chainId", signer.getChainId());
            domain.put("verifyingContract", exchangeAddress);
            typedData.put("domain", domain);

            // Types
            Map<String, List<Map<String, String>>> types = new HashMap<>();

            List<Map<String, String>> domainType = Arrays.asList(
                    createType("name", "string"),
                    createType("version", "string"),
                    createType("chainId", "uint256"),
                    createType("verifyingContract", "address")
            );
            types.put("EIP712Domain", domainType);

            // V2 Order struct - different from V1 (no taker/nonce/feeRateBps/expiration, adds timestamp/metadata/builder)
            List<Map<String, String>> orderType = Arrays.asList(
                    createType("salt", "uint256"),
                    createType("maker", "address"),
                    createType("signer", "address"),
                    createType("tokenId", "uint256"),
                    createType("makerAmount", "uint256"),
                    createType("takerAmount", "uint256"),
                    createType("side", "uint8"),
                    createType("signatureType", "uint8"),
                    createType("timestamp", "uint256"),
                    createType("metadata", "bytes32"),
                    createType("builder", "bytes32")
            );
            types.put("Order", orderType);
            typedData.put("types", types);

            // Message - expiration is NOT included in the EIP712 hash for V2
            Map<String, Object> message = new HashMap<>();
            message.put("salt", order.getSalt());
            message.put("maker", order.getMaker());
            message.put("signer", order.getSigner());
            message.put("tokenId", order.getTokenId());
            message.put("makerAmount", order.getMakerAmount());
            message.put("takerAmount", order.getTakerAmount());
            message.put("side", order.getSide());
            message.put("signatureType", order.getSignatureType());
            message.put("timestamp", order.getTimestamp());
            // bytes32 values: pass as hex string without 0x prefix for StructuredDataEncoder.
            // Both default to BYTES32_ZERO ("0x000...0") but guard against null just in case.
            String metadataHex = order.getMetadata() != null
                    ? stripHexPrefix(order.getMetadata()).toLowerCase()
                    : "0000000000000000000000000000000000000000000000000000000000000000";
            String builderHex = order.getBuilder() != null
                    ? stripHexPrefix(order.getBuilder()).toLowerCase()
                    : "0000000000000000000000000000000000000000000000000000000000000000";
            message.put("metadata", metadataHex);
            message.put("builder", builderHex);
            typedData.put("message", message);

            typedData.put("primaryType", "Order");

            String jsonTypedData = objectMapper.writeValueAsString(typedData);
            StructuredDataEncoder encoder = new StructuredDataEncoder(jsonTypedData);

            byte[] structHash = encoder.hashStructuredData();

            String signature = signer.sign(structHash);
            return signature.startsWith("0x") ? signature : "0x" + signature;

        } catch (IOException e) {
            throw new RuntimeException("Failed to sign V2 order", e);
        }
    }

    /**
     * Sign a V1 order using EIP-712
     *
     * @param order   The V1 order to sign
     * @param negRisk Whether this is a negative risk market
     * @return The signature as a hex string
     */
    private String signOrder(Order order, boolean negRisk) {
        try {
            ContractConfig config = Config.getContractConfig(signer.getChainId(), negRisk);
            String exchangeAddress = negRisk ? config.getNegRiskExchange() : config.getExchange();

            Map<String, Object> typedData = new HashMap<>();

            // Domain - version "1" for V1 orders
            Map<String, Object> domain = new HashMap<>();
            domain.put("name", "Polymarket CTF Exchange");
            domain.put("version", "1");
            domain.put("chainId", signer.getChainId());
            domain.put("verifyingContract", exchangeAddress);
            typedData.put("domain", domain);

            // Types
            Map<String, List<Map<String, String>>> types = new HashMap<>();

            List<Map<String, String>> domainType = Arrays.asList(
                    createType("name", "string"),
                    createType("version", "string"),
                    createType("chainId", "uint256"),
                    createType("verifyingContract", "address")
            );
            types.put("EIP712Domain", domainType);

            // V1 Order struct
            List<Map<String, String>> orderType = Arrays.asList(
                    createType("salt", "uint256"),
                    createType("maker", "address"),
                    createType("signer", "address"),
                    createType("taker", "address"),
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

            Map<String, Object> message = new HashMap<>();
            message.put("salt", order.getSalt());
            message.put("maker", order.getMaker());
            message.put("signer", order.getSigner());
            message.put("taker", order.getTaker());
            message.put("tokenId", order.getTokenId());
            message.put("makerAmount", order.getMakerAmount());
            message.put("takerAmount", order.getTakerAmount());
            message.put("expiration", order.getExpiration());
            message.put("nonce", order.getNonce());
            message.put("feeRateBps", order.getFeeRateBps());
            message.put("side", order.getSide());
            message.put("signatureType", order.getSignatureType());
            typedData.put("message", message);

            typedData.put("primaryType", "Order");

            String jsonTypedData = objectMapper.writeValueAsString(typedData);
            StructuredDataEncoder encoder = new StructuredDataEncoder(jsonTypedData);

            byte[] structHash = encoder.hashStructuredData();

            String signature = signer.sign(structHash);
            return signature.startsWith("0x") ? signature : "0x" + signature;

        } catch (IOException e) {
            throw new RuntimeException("Failed to sign order", e);
        }
    }

    /**
     * Generate a random salt value (matches Python generate_seed)
     */
    private BigInteger generateSalt() {
        long now = System.currentTimeMillis();
        double timestamp = now / 1000.0;
        long seed = Math.round(timestamp * random.nextDouble());
        return BigInteger.valueOf(seed);
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

    private static double roundDown(double x, int sigDigits) {
        double multiplier = Math.pow(10, sigDigits);
        return Math.floor(x * multiplier) / multiplier;
    }

    private static double roundNormal(double x, int sigDigits) {
        double multiplier = Math.pow(10, sigDigits);
        return Math.round(x * multiplier) / multiplier;
    }

    private static double roundUp(double x, int sigDigits) {
        double multiplier = Math.pow(10, sigDigits);
        return Math.ceil(x * multiplier) / multiplier;
    }

    private static long toTokenDecimals(double x) {
        double f = 1000000.0 * x;
        if (decimalPlaces(f) > 0) {
            f = roundNormal(f, 0);
        }
        return (long) f;
    }

    private static int decimalPlaces(double x) {
        BigDecimal bd = BigDecimal.valueOf(x);
        return Math.max(0, bd.stripTrailingZeros().scale());
    }

    private static String normalizeAddress(String address) {
        if (address == null) {
            return null;
        }
        return address.startsWith("0x") ? address : "0x" + address;
    }

    private static String stripHexPrefix(String hexString) {
        if (hexString == null) {
            return null;
        }
        return hexString.startsWith("0x") ? hexString.substring(2) : hexString;
    }
}


