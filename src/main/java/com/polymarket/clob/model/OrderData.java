package com.polymarket.clob.model;

import com.polymarket.clob.Constants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

/**
 * Inputs to generate orders
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderData {
    /**
     * Unique salt to ensure entropy (optional, will be generated if not provided)
     */
    private BigInteger salt;

    /**
     * Maker of the order, i.e the source of funds for the order
     */
    private String maker;

    /**
     * Address of the order taker. The zero address is used to indicate a public order
     */
    @Builder.Default
    private String taker = Constants.ZERO_ADDRESS;

    /**
     * Token Id of the CTF ERC1155 asset to be bought or sold.
     * If BUY, this is the tokenId of the asset to be bought, i.e the makerAssetId
     * If SELL, this is the tokenId of the asset to be sold, i.e the takerAssetId
     */
    private String tokenId;

    /**
     * Maker amount, i.e the max amount of tokens to be sold
     */
    private String makerAmount;

    /**
     * Taker amount, i.e the minimum amount of tokens to be received
     */
    private String takerAmount;

    /**
     * The side of the order, BUY or SELL (stored as integer: 0=BUY, 1=SELL)
     */
    private Integer side;

    /**
     * Fee rate, in basis points, charged to the order maker, charged on proceeds
     */
    private String feeRateBps;

    /**
     * Nonce used for onchain cancellations
     */
    @Builder.Default
    private String nonce = "0";

    /**
     * Signer of the order. Optional, if it is not present the signer is the maker of the order.
     */
    private String signer;

    /**
     * Timestamp after which the order is expired.
     * Optional, if it is not present the value is '0' (no expiration)
     */
    @Builder.Default
    private String expiration = "0";

    /**
     * Signature type used by the Order. Default value 'EOA' (0)
     */
    @Builder.Default
    private Integer signatureType = 0;
}
