package com.polymarket.clob.signing;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * CLOB authentication message structure (EIP-712)
 */
@Data
@AllArgsConstructor
public class ClobAuth {
    private String address;
    private String timestamp;
    private long nonce;
    private String message;
    
    public static final String MSG_TO_SIGN = "This message attests that I control the given wallet";
    public static final String CLOB_DOMAIN_NAME = "ClobAuthDomain";
    public static final String CLOB_VERSION = "1";
}
