package com.polymarket.clob.model;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A signed order ready to be posted to the CLOB
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignedOrder {
    /**
     * The salt value for uniqueness
     */
    private String salt;

    /**
     * The maker address
     */
    private String maker;

    /**
     * The signer address
     */
    private String signer;

    /**
     * The taker address (zero address for public orders)
     */
    private String taker;

    /**
     * Token ID being traded
     */
    @JSONField(name = "tokenId")
    private String tokenId;

    /**
     * The maker amount
     */
    @JSONField(name = "makerAmount")
    private String makerAmount;

    /**
     * The taker amount
     */
    @JSONField(name = "takerAmount")
    private String takerAmount;

    /**
     * Expiration timestamp
     */
    private String expiration;

    /**
     * Nonce for cancellation
     */
    private String nonce;

    /**
     * Fee rate in basis points
     */
    @JSONField(name = "feeRateBps")
    private String feeRateBps;

    /**
     * Side of the order (BUY/SELL)
     */
    private String side;

    /**
     * Signature type (0 for EOA, 1 for Poly Proxy, 2 for Poly Gnosis Safe)
     */
    @JSONField(name = "signatureType")
    private int signatureType;

    /**
     * The signature
     */
    private String signature;
}

