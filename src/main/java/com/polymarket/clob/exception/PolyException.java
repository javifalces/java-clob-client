package com.polymarket.clob.exception;

/**
 * Exception thrown when CLOB API operations fail
 */
public class PolyException extends RuntimeException {
    
    public PolyException(String message) {
        super(message);
    }
    
    public PolyException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public PolyException(Throwable cause) {
        super(cause);
    }
}
