package com.polymarket.clob.signing;

import org.web3j.crypto.StructuredDataEncoder;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Arrays;

/**
 * EIP-712 signature utilities for CLOB authentication
 */
public class Eip712 {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Get the CLOB authentication domain
     * 
     * @param chainId The chain ID
     * @return Domain map
     */
    public static Map<String, Object> getClobAuthDomain(int chainId) {
        Map<String, Object> domain = new HashMap<>();
        domain.put("name", ClobAuth.CLOB_DOMAIN_NAME);
        domain.put("version", ClobAuth.CLOB_VERSION);
        domain.put("chainId", chainId);
        return domain;
    }
    
    /**
     * Sign a CLOB authentication message
     * 
     * @param signer The signer
     * @param timestamp The timestamp
     * @param nonce The nonce
     * @return The signature as a hex string
     */
    public static String signClobAuthMessage(Signer signer, long timestamp, long nonce) {
        try {
            // Create the CLOB auth message
            ClobAuth clobAuth = new ClobAuth(
                signer.getAddress(),
                String.valueOf(timestamp),
                nonce,
                ClobAuth.MSG_TO_SIGN
            );
            
            // Build EIP-712 typed data
            Map<String, Object> typedData = new HashMap<>();
            typedData.put("domain", getClobAuthDomain(signer.getChainId()));
            
            // Define types
            Map<String, List<Map<String, String>>> types = new HashMap<>();

            // Define EIP712Domain type
            List<Map<String, String>> domainType = Arrays.asList(
                    createType("name", "string"),
                    createType("version", "string"),
                    createType("chainId", "uint256")
            );
            types.put("EIP712Domain", domainType);

            // Define ClobAuth type
            List<Map<String, String>> clobAuthType = Arrays.asList(
                createType("address", "address"),
                createType("timestamp", "string"),
                createType("nonce", "uint256"),
                createType("message", "string")
            );
            types.put("ClobAuth", clobAuthType);
            typedData.put("types", types);
            
            // Add message
            Map<String, Object> message = new HashMap<>();
            message.put("address", clobAuth.getAddress());
            message.put("timestamp", clobAuth.getTimestamp());
            message.put("nonce", clobAuth.getNonce());
            message.put("message", clobAuth.getMessage());
            typedData.put("message", message);
            
            // Primary type
            typedData.put("primaryType", "ClobAuth");
            
            // Convert to JSON and create structured data encoder
            String jsonTypedData = objectMapper.writeValueAsString(typedData);
            StructuredDataEncoder encoder = new StructuredDataEncoder(jsonTypedData);
            
            // Get the hash to sign
            byte[] structHash = encoder.hashStructuredData();
            
            // Sign and return with 0x prefix
            String signature = signer.sign(structHash);
            return signature.startsWith("0x") ? signature : "0x" + signature;
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to sign CLOB auth message", e);
        }
    }
    
    private static Map<String, String> createType(String name, String type) {
        Map<String, String> typeMap = new HashMap<>();
        typeMap.put("name", name);
        typeMap.put("type", type);
        return typeMap;
    }
}
