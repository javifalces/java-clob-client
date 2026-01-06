package com.polymarket.clob.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Arguments for posting multiple orders
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostOrdersArgs {
    /**
     * The signed order to post
     */
    private SignedOrder order;

    /**
     * The order type
     */
    @Builder.Default
    private OrderType orderType = OrderType.GTC;

    /**
     * Whether this is a post-only order
     */
    @Builder.Default
    private boolean postOnly = false;
}

