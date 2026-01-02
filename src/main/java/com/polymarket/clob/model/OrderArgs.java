package com.polymarket.clob.model;

import com.polymarket.clob.Constants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Arguments for creating an order
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderArgs {
    /**
     * TokenID of the Conditional token asset being traded
     */
    private String tokenId;
    
    /**
     * Price used to create the order
     */
    private double price;
    
    /**
     * Size in terms of the ConditionalToken
     */
    private double size;
    
    /**
     * Side of the order (BUY or SELL)
     */
    private String side;
    
    /**
     * Fee rate, in basis points, charged to the order maker, charged on proceeds
     */
    @Builder.Default
    private int feeRateBps = 0;
    
    /**
     * Nonce used for onchain cancellations
     */
    @Builder.Default
    private int nonce = 0;
    
    /**
     * Timestamp after which the order is expired
     */
    @Builder.Default
    private long expiration = 0;
    
    /**
     * Address of the order taker. The zero address is used to indicate a public order
     */
    @Builder.Default
    private String taker = Constants.ZERO_ADDRESS;
}
