package com.polymarket.clob.signing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.polymarket.clob.Constants;
import com.polymarket.clob.config.Config;
import com.polymarket.clob.model.*;
import org.web3j.crypto.StructuredDataEncoder;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.*;

/**
 * Builds and signs orders for the CLOB
 */
public class OrderBuilder {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final SecureRandom random = new SecureRandom();

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
     * Create a new OrderBuilder with default signature type (EOA)
     */
    public OrderBuilder(Signer signer) {
        this(signer, 0, null);
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
        String salt = generateSalt();

        // Calculate amounts
        BigDecimal price = new BigDecimal(String.valueOf(orderArgs.getPrice()));
        BigDecimal size = new BigDecimal(String.valueOf(orderArgs.getSize()));

        String makerAmount;
        String takerAmount;

        if (Constants.BUY.equals(orderArgs.getSide())) {
            // For BUY orders: maker pays price*size, receives size
            makerAmount = price.multiply(size).setScale(6, RoundingMode.HALF_UP).toPlainString();
            takerAmount = size.setScale(6, RoundingMode.HALF_UP).toPlainString();
        } else {
            // For SELL orders: maker pays size, receives price*size
            makerAmount = size.setScale(6, RoundingMode.HALF_UP).toPlainString();
            takerAmount = price.multiply(size).setScale(6, RoundingMode.HALF_UP).toPlainString();
        }

        // Build the order
        SignedOrder order = SignedOrder.builder()
                .salt(salt)
                .maker(funder)
                .signer(signer.getAddress())
                .taker(orderArgs.getTaker())
                .tokenId(orderArgs.getTokenId())
                .makerAmount(makerAmount)
                .takerAmount(takerAmount)
                .expiration(String.valueOf(orderArgs.getExpiration()))
                .nonce(String.valueOf(orderArgs.getNonce()))
                .feeRateBps(String.valueOf(orderArgs.getFeeRateBps()))
                .side(orderArgs.getSide())
                .signatureType(signatureType)
                .build();

        // Sign the order
        String signature = signOrder(order, options.isNegRisk());
        order.setSignature(signature);

        return order;
    }

    /**
     * Create and sign a market order
     *
     * @param orderArgs The market order arguments
     * @param options   The creation options
     * @return A signed order ready to post
     */
    public SignedOrder createMarketOrder(MarketOrderArgs orderArgs, CreateOrderOptions options) {
        // Convert MarketOrderArgs to OrderArgs
        OrderArgs standardArgs = OrderArgs.builder()
                .tokenId(orderArgs.getTokenId())
                .price(orderArgs.getPrice())
                .size(orderArgs.getAmount())
                .side(orderArgs.getSide())
                .feeRateBps(orderArgs.getFeeRateBps())
                .nonce(orderArgs.getNonce())
                .expiration(0)  // Market orders typically don't have expiration
                .taker(orderArgs.getTaker() != null ? orderArgs.getTaker() : Constants.ZERO_ADDRESS)
                .build();

        return createOrder(standardArgs, options);
    }

    /**
     * Sign an order using EIP-712
     *
     * @param order   The order to sign
     * @param negRisk Whether this is a negative risk market
     * @return The signature as a hex string
     */
    private String signOrder(SignedOrder order, boolean negRisk) {
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

            // Message
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
            message.put("side", Constants.BUY.equals(order.getSide()) ? 0 : 1);
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
    private String generateSalt() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        BigInteger salt = new BigInteger(1, bytes);
        return salt.toString();
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
}

