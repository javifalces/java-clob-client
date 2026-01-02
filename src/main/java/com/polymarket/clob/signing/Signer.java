package com.polymarket.clob.signing;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

/**
 * Handles signing operations for the CLOB client
 */
public class Signer {
    
    private final String privateKey;
    private final Credentials credentials;
    private final int chainId;
    
    /**
     * Creates a new Signer instance
     * 
     * @param privateKey The private key (with or without 0x prefix)
     * @param chainId The chain ID
     */
    public Signer(String privateKey, int chainId) {
        if (privateKey == null || chainId <= 0) {
            throw new IllegalArgumentException("Private key and chain ID must be provided");
        }
        
        // Ensure private key has 0x prefix
        this.privateKey = privateKey.startsWith("0x") ? privateKey : "0x" + privateKey;
        
        // Create credentials from private key
        this.credentials = Credentials.create(this.privateKey);
        this.chainId = chainId;
    }
    
    /**
     * Get the Ethereum address associated with this signer
     * 
     * @return The Ethereum address
     */
    public String getAddress() {
        return credentials.getAddress();
    }
    
    /**
     * Get the chain ID
     * 
     * @return The chain ID
     */
    public int getChainId() {
        return chainId;
    }
    
    /**
     * Sign a message hash
     * 
     * @param messageHash The hash to sign
     * @return The signature as a hex string
     */
    public String sign(byte[] messageHash) {
        Sign.SignatureData signature = Sign.signMessage(messageHash, credentials.getEcKeyPair(), false);
        
        // Combine r, s, and v into a single byte array
        byte[] retval = new byte[65];
        System.arraycopy(signature.getR(), 0, retval, 0, 32);
        System.arraycopy(signature.getS(), 0, retval, 32, 32);
        System.arraycopy(signature.getV(), 0, retval, 64, 1);
        
        return Numeric.toHexString(retval);
    }
    
    /**
     * Sign a message hash given as a hex string
     * 
     * @param messageHashHex The hash to sign as a hex string
     * @return The signature as a hex string
     */
    public String sign(String messageHashHex) {
        byte[] messageHash = Numeric.hexStringToByteArray(messageHashHex);
        return sign(messageHash);
    }
    
    /**
     * Get the credentials object
     * 
     * @return The Web3j Credentials object
     */
    public Credentials getCredentials() {
        return credentials;
    }
}
