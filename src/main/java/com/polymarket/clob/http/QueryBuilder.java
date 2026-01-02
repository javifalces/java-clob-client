package com.polymarket.clob.http;

import com.polymarket.clob.model.*;

/**
 * Utilities for building query parameters
 */
public class QueryBuilder {
    
    /**
     * Build query parameters for a URL
     * 
     * @param url The base URL
     * @param param The parameter name
     * @param val The parameter value
     * @return URL with appended parameter
     */
    public static String buildQueryParams(String url, String param, String val) {
        if (val == null) {
            return url;
        }
        
        String urlWithParams = url;
        char last = urlWithParams.charAt(urlWithParams.length() - 1);
        
        // If last character is '?', append param directly
        if (last == '?') {
            urlWithParams = urlWithParams + param + "=" + val;
        } else {
            // Otherwise add '&' before the param
            urlWithParams = urlWithParams + "&" + param + "=" + val;
        }
        
        return urlWithParams;
    }
    
    /**
     * Add trade query parameters to URL
     * 
     * @param baseUrl The base URL
     * @param params The trade parameters (can be null)
     * @param nextCursor The pagination cursor (default "MA==")
     * @return URL with query parameters
     */
    public static String addQueryTradeParams(String baseUrl, TradeParams params, String nextCursor) {
        String url = baseUrl;
        
        // Check if we need to add query parameters
        boolean hasQuery = (nextCursor != null && !nextCursor.isEmpty()) || 
            (params != null && (params.getMarket() != null || params.getAssetId() != null || 
             params.getAfter() != null || params.getBefore() != null || 
             params.getMakerAddress() != null || params.getId() != null));
        
        if (hasQuery) {
            url = url + "?";
        }
        
        if (params != null) {
            if (params.getMarket() != null) {
                url = buildQueryParams(url, "market", params.getMarket());
            }
            if (params.getAssetId() != null) {
                url = buildQueryParams(url, "asset_id", params.getAssetId());
            }
            if (params.getAfter() != null) {
                url = buildQueryParams(url, "after", String.valueOf(params.getAfter()));
            }
            if (params.getBefore() != null) {
                url = buildQueryParams(url, "before", String.valueOf(params.getBefore()));
            }
            if (params.getMakerAddress() != null) {
                url = buildQueryParams(url, "maker_address", params.getMakerAddress());
            }
            if (params.getId() != null) {
                url = buildQueryParams(url, "id", params.getId());
            }
        }
        
        if (nextCursor != null && !nextCursor.isEmpty()) {
            url = buildQueryParams(url, "next_cursor", nextCursor);
        }
        
        return url;
    }
    
    /**
     * Add open order query parameters to URL
     * 
     * @param baseUrl The base URL
     * @param params The open order parameters (can be null)
     * @param nextCursor The pagination cursor (default "MA==")
     * @return URL with query parameters
     */
    public static String addQueryOpenOrdersParams(String baseUrl, OpenOrderParams params, String nextCursor) {
        String url = baseUrl;
        
        // Check if we need to add query parameters
        boolean hasQuery = (nextCursor != null && !nextCursor.isEmpty()) || 
            (params != null && (params.getMarket() != null || params.getAssetId() != null || 
             params.getId() != null));
        
        if (hasQuery) {
            url = url + "?";
        }
        
        if (params != null) {
            if (params.getMarket() != null) {
                url = buildQueryParams(url, "market", params.getMarket());
            }
            if (params.getAssetId() != null) {
                url = buildQueryParams(url, "asset_id", params.getAssetId());
            }
            if (params.getId() != null) {
                url = buildQueryParams(url, "id", params.getId());
            }
        }
        
        if (nextCursor != null && !nextCursor.isEmpty()) {
            url = buildQueryParams(url, "next_cursor", nextCursor);
        }
        
        return url;
    }
}
