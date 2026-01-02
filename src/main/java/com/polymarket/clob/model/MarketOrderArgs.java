package com.polymarket.clob.model;

import com.polymarket.clob.Constants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Arguments for creating a market order
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketOrderArgs {
    /**
     * TokenID of the Conditional token asset being traded
     */
    private String tokenId;
    
    /**
     * BUY orders: $$$ Amount to buy
     * SELL orders: Shares to sell
     */
    private double amount;
    
    /**
     * Side of the order (BUY or SELL)
     */
    private String side;
    
    /**
     * Price used to create the order (optional for market orders)
     */
    @Builder.Default
    private double price = 0;
    
    /**
     * Fee rate, in basis points, charged to the order maker
     */
    @Builder.Default
    private int feeRateBps = 0;
    
    /**
     * Nonce used for onchain cancellations
     */
    @Builder.Default
    private int nonce = 0;
    
    /**
     * Address of the order taker
     */
    @Builder.Default
    private String taker = Constants.ZERO_ADDRESS;
    
    /**
     * Order type (default: FOK for market orders)
     */
    @Builder.Default
    private OrderType orderType = OrderType.FOK;
}
