package com.polymarket.clob.model;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Order + Signature
 * A signed order ready to be posted to the CLOB
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignedOrder {
    /**
     * The order
     */
    private Order order;

    /**
     * The signature
     */
    private String signature;

    /**
     * Convert to dictionary representation
     * Returns format with nested "order" object
     */
    public Map<String, Object> toDict() {
        Map<String, Object> orderDict = order.toDict();

        // Convert side from integer to string
        int side = order.getSide();
        if (side == 0) {
            orderDict.put("side", "BUY");
        } else {
            orderDict.put("side", "SELL");
        }

        // Ensure all numeric values are strings
        orderDict.put("expiration", String.valueOf(order.getExpiration()));
        orderDict.put("nonce", String.valueOf(order.getNonce()));
        orderDict.put("feeRateBps", String.valueOf(order.getFeeRateBps()));
        orderDict.put("makerAmount", String.valueOf(order.getMakerAmount()));
        orderDict.put("takerAmount", String.valueOf(order.getTakerAmount()));
        orderDict.put("tokenId", String.valueOf(order.getTokenId()));

        // Add signature to the order dict (not at top level)
        orderDict.put("signature", signature);

        // Wrap in "order" key
        Map<String, Object> result = new HashMap<>();
        result.put("order", orderDict);

        return result;
    }

    public String getMaker() {
        return order != null ? order.getMaker() : null;
    }

    public String getSigner() {
        return order != null ? order.getSigner() : null;
    }

    public String getTaker() {
        return order != null ? order.getTaker() : null;
    }

    @JSONField(name = "tokenId")
    public String getTokenId() {
        return order != null ? order.getTokenId() : null;
    }

    @JSONField(name = "makerAmount")
    public String getMakerAmount() {
        return order != null ? order.getMakerAmount() : null;
    }

    @JSONField(name = "takerAmount")
    public String getTakerAmount() {
        return order != null ? order.getTakerAmount() : null;
    }

    public String getExpiration() {
        return order != null ? order.getExpiration() : null;
    }

    public String getNonce() {
        return order != null ? order.getNonce() : null;
    }

    @JSONField(name = "feeRateBps")
    public String getFeeRateBps() {
        return order != null ? order.getFeeRateBps() : null;
    }

    public String getSide() {
        if (order == null) return null;
        return order.getSide() == 0 ? "BUY" : "SELL";
    }


    public void setMaker(String maker) {
        if (order == null) order = new Order();
        order.setMaker(maker);
    }

    public void setSigner(String signer) {
        if (order == null) order = new Order();
        order.setSigner(signer);
    }

    public void setTaker(String taker) {
        if (order == null) order = new Order();
        order.setTaker(taker);
    }

    public void setTokenId(String tokenId) {
        if (order == null) order = new Order();
        order.setTokenId(tokenId);
    }

    public void setMakerAmount(String makerAmount) {
        if (order == null) order = new Order();
        order.setMakerAmount(makerAmount);
    }

    public void setTakerAmount(String takerAmount) {
        if (order == null) order = new Order();
        order.setTakerAmount(takerAmount);
    }

    public void setExpiration(String expiration) {
        if (order == null) order = new Order();
        order.setExpiration(expiration);
    }

    public void setNonce(String nonce) {
        if (order == null) order = new Order();
        order.setNonce(nonce);
    }

    public void setFeeRateBps(String feeRateBps) {
        if (order == null) order = new Order();
        order.setFeeRateBps(feeRateBps);
    }

    public void setSide(String side) {
        if (order == null) order = new Order();
        order.setSide("BUY".equals(side) ? 0 : 1);
    }

    public void setSignatureType(int signatureType) {
        if (order == null) order = new Order();
        order.setSignatureType(signatureType);
    }
}

