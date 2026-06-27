package com.polymarket.clob;

/**
 * Constants used throughout the CLOB client
 */
public final class Constants {
    
    private Constants() {
        // Prevent instantiation
    }
    
    // Access levels
    public static final int L0 = 0;
    public static final int L1 = 1;
    public static final int L2 = 2;
    
    // Warnings and error messages
    public static final String CREDENTIAL_CREATION_WARNING = 
        """
        🚨🚨🚨
        Your credentials CANNOT be recovered after they've been created.
        Be sure to store them safely!
        🚨🚨🚨""";
    
    public static final String L1_AUTH_UNAVAILABLE = 
        "A private key is needed to interact with this endpoint!";
    
    public static final String L2_AUTH_UNAVAILABLE = 
        "API Credentials are needed to interact with this endpoint!";
    
    public static final String BUILDER_AUTH_UNAVAILABLE = 
        "Builder API Credentials needed to interact with this endpoint!";
    
    // Ethereum zero address
    public static final String ZERO_ADDRESS = "0x0000000000000000000000000000000000000000";

    // Order sides
    public static final String BUY = "BUY";
    public static final String SELL = "SELL";

    // Chain IDs
    public static final int AMOY = 80002;
    public static final int POLYGON = 137;

    // Default RPC URLs
    public static final String POLYGON_RPC_URL = "https://polygon-bor-rpc.publicnode.com";
    public static final String AMOY_RPC_URL = "https://rpc-amoy.polygon.technology";
    
    // Pagination
    public static final String END_CURSOR = "LTE=";
    public static final String INITIAL_CURSOR = "MA==";

    // Bytes32 zero value
    public static final String BYTES32_ZERO = "0x0000000000000000000000000000000000000000000000000000000000000000";

    // Error messages
    public static final String ORDER_VERSION_MISMATCH_ERROR = "order version mismatch";
}
