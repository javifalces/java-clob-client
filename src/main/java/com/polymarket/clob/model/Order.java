package com.polymarket.clob.model;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * Order - represents the EIP712 struct
 * NOTE: Important to keep in mind, fields are ordered
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
    /**
     * Unique salt to ensure entropy
     */
    private BigInteger salt;

    /**
     * Maker of the order, i.e the source of funds for the order
     */
    private String maker;

    /**
     * Signer of the order
     */
    private String signer;

    /**
     * Address of the order taker. The zero address is used to indicate a public order
     */
    private String taker;

    /**
     * Token Id of the CTF ERC1155 asset to be bought or sold.
     * If BUY, this is the tokenId of the asset to be bought, i.e the makerAssetId
     * If SELL, this is the tokenId of the asset to be sold, i.e the takerAssetId
     */
    @JSONField(name = "tokenId")
    private String tokenId;

    /**
     * Maker amount, i.e the max amount of tokens to be sold
     */
    @JSONField(name = "makerAmount")
    private String makerAmount;

    /**
     * Taker amount, i.e the minimum amount of tokens to be received
     */
    @JSONField(name = "takerAmount")
    private String takerAmount;

    /**
     * Timestamp after which the order is expired
     */
    private String expiration;

    /**
     * Nonce used for onchain cancellations
     */
    private String nonce;

    /**
     * Fee rate, in basis points, charged to the order maker, charged on proceeds
     */
    private String feeRateBps;

    /**
     * The side of the order, BUY or SELL (stored as integer: 0=BUY, 1=SELL)
     */
    private int side;

    /**
     * Signature type used by the Order
     */
    private int signatureType;

    /**
     * Convert to dictionary representation
     */
    public Map<String, Object> toDict() {
        Map<String, Object> dict = new HashMap<>();
        dict.put("salt", salt);
        dict.put("maker", maker);
        dict.put("signer", signer);
        dict.put("taker", taker);
        dict.put("tokenId", tokenId);
        dict.put("makerAmount", makerAmount);
        dict.put("takerAmount", takerAmount);
        dict.put("expiration", expiration);
        dict.put("nonce", nonce);
        dict.put("feeRateBps", feeRateBps);
        dict.put("side", side);
        dict.put("signatureType", signatureType);
        return dict;
    }
}
