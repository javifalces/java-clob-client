package com.polymarket.clob.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * V2 Order + Signature.
 * A signed V2 order ready to be posted to the CLOB.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignedOrderV2 {
    /**
     * The V2 order
     */
    private OrderV2 order;

    /**
     * The EIP712 signature
     */
    private String signature;

    // ==================== Convenience accessor methods ====================

    public String getMaker() {
        return order != null ? order.getMaker() : null;
    }

    public String getSigner() {
        return order != null ? order.getSigner() : null;
    }

    public String getTokenId() {
        return order != null ? order.getTokenId() : null;
    }

    public String getMakerAmount() {
        return order != null ? order.getMakerAmount() : null;
    }

    public String getTakerAmount() {
        return order != null ? order.getTakerAmount() : null;
    }

    public String getExpiration() {
        return order != null ? order.getExpiration() : null;
    }

    public String getTimestamp() {
        return order != null ? order.getTimestamp() : null;
    }

    public String getMetadata() {
        return order != null ? order.getMetadata() : null;
    }

    public String getBuilderCode() {
        return order != null ? order.getBuilder() : null;
    }

    public String getSide() {
        if (order == null) return null;
        return order.getSide() == 0 ? "BUY" : "SELL";
    }

    /**
     * Convert to the JSON dictionary representation expected by the CLOB API.
     * Returns a map with a nested "order" object.
     */
    public Map<String, Object> toDict() {
        Map<String, Object> orderDict = new HashMap<>();
        orderDict.put("salt", order.getSalt());
        orderDict.put("maker", order.getMaker());
        orderDict.put("signer", order.getSigner());
        orderDict.put("tokenId", order.getTokenId());
        orderDict.put("makerAmount", order.getMakerAmount());
        orderDict.put("takerAmount", order.getTakerAmount());
        orderDict.put("side", order.getSide() == 0 ? "BUY" : "SELL");
        orderDict.put("expiration", order.getExpiration());
        orderDict.put("signatureType", order.getSignatureType());
        orderDict.put("timestamp", order.getTimestamp());
        orderDict.put("metadata", order.getMetadata());
        orderDict.put("builder", order.getBuilder());
        orderDict.put("signature", signature);

        Map<String, Object> result = new HashMap<>();
        result.put("order", orderDict);
        return result;
    }
}
