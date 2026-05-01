package com.polymarket.clob.model;

import com.polymarket.clob.Constants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Arguments for creating an order (V2 by default).
 * <p>
 * V2 fields: tokenId, price, size, side, expiration, builderCode, metadata.
 * Legacy V1 fields (feeRateBps, nonce, taker) are retained for backward compatibility
 * but are ignored when creating V2 orders.
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
     * Timestamp after which the order is expired (0 = no expiration)
     */
    @Builder.Default
    private long expiration = 0;

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
