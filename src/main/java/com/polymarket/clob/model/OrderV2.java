package com.polymarket.clob.model;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

/**
 * V2 Order - represents the EIP712 struct for the V2 exchange contract.
 * <p>
 * Key differences from V1:
 * - Adds timestamp, metadata, builder fields
 * - Removes taker, nonce, feeRateBps fields
 * - expiration is in JSON payload but NOT part of the EIP712 hash
 * NOTE: Field order matches the V2 EIP712 struct definition.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderV2 {
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
     * Token Id of the CTF ERC1155 asset to be bought or sold.
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
     * The side of the order, BUY or SELL (stored as integer: 0=BUY, 1=SELL)
     */
    private int side;

    /**
     * Signature type used by the Order
     */
    private int signatureType;

    /**
     * Timestamp in milliseconds when the order was created
     */
    private String timestamp;

    /**
     * Optional metadata (bytes32 hex) attached to the order
     */
    private String metadata;

    /**
     * Builder code (bytes32 hex) for builder fee attribution
     */
    private String builder;

    /**
     * Timestamp after which the order is expired (NOT part of EIP712 hash, only in JSON payload)
     */
    @Builder.Default
    private String expiration = "0";
}
