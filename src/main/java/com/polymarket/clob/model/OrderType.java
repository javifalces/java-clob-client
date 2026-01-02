package com.polymarket.clob.model;

/**
 * Order type enumeration
 */
public enum OrderType {
    /**
     * Good 'til Canceled - Order remains active until filled or canceled
     */
    GTC,
    
    /**
     * Fill or Kill - Order must be filled immediately in its entirety or canceled
     */
    FOK,
    
    /**
     * Good 'til Date - Order is active until a specified date
     */
    GTD,
    
    /**
     * Fill and Kill (Immediate or Cancel) - Fill as much as possible immediately, cancel the rest
     */
    FAK
}
