package com.polymarket.clob.model;

import com.polymarket.clob.Constants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Arguments for creating a market order (V2 by default).
 * <p>
 * V2 fields: tokenId, amount, side, price, orderType, builderCode, metadata.
 * Legacy V1 fields (feeRateBps, nonce, taker) are retained for backward compatibility
 * but are ignored when creating V2 orders.
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
     * Price used to create the order (optional for market orders, auto-calculated if 0)
     */
    @Builder.Default
    private double price = 0;

    /**
     * Order type (default: FOK for market orders)
     */
    @Builder.Default
    private OrderType orderType = OrderType.FOK;

    /**
     * Builder code (bytes32 hex) for builder fee attribution (V2)
     */
    @Builder.Default
    private String builderCode = Constants.BYTES32_ZERO;

    /**
     * Optional metadata (bytes32 hex) attached to the order (V2)
     */
    @Builder.Default
    private String metadata = Constants.BYTES32_ZERO;

    // ==================== Legacy V1 fields (kept for backward compatibility) ====================

    /**
     * Fee rate, in basis points, charged to the order maker (V1 only)
     *
     * @deprecated Used only for V1 orders. Ignored in V2 order creation.
     */
    @Deprecated
    @Builder.Default
    private int feeRateBps = 0;
    
    /**
     * Nonce used for onchain cancellations (V1 only)
     *
     * @deprecated Used only for V1 orders. Ignored in V2 order creation.
     */
    @Deprecated
    @Builder.Default
    private int nonce = 0;
    
    /**
     * Address of the order taker (V1 only). Zero address = public order.
     *
     * @deprecated Used only for V1 orders. Ignored in V2 order creation.
     */
    @Deprecated
    @Builder.Default
    private String taker = Constants.ZERO_ADDRESS;
}
