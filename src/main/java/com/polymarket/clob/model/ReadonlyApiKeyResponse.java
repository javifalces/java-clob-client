package com.polymarket.clob.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Readonly API key response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReadonlyApiKeyResponse {
    private String apiKey;
}
