package com.polymarket.clob;

/**
 * API Endpoints for the CLOB service
 */
public final class Endpoints {
    
    private Endpoints() {
        // Prevent instantiation
    }

    // Health check
    public static final String OK = "/ok";
    
    // Server time and version
    public static final String TIME = "/time";
    public static final String VERSION = "/version";
    
    // Authentication endpoints
    public static final String CREATE_API_KEY = "/auth/api-key";
    public static final String GET_API_KEYS = "/auth/api-keys";
    public static final String DELETE_API_KEY = "/auth/api-key";
    public static final String DERIVE_API_KEY = "/auth/derive-api-key";
    public static final String CLOSED_ONLY = "/auth/ban-status/closed-only";
    
    // Readonly API Key endpoints
    public static final String CREATE_READONLY_API_KEY = "/auth/readonly-api-key";
    public static final String GET_READONLY_API_KEYS = "/auth/readonly-api-keys";
    public static final String DELETE_READONLY_API_KEY = "/auth/readonly-api-key";
    public static final String VALIDATE_READONLY_API_KEY = "/auth/validate-readonly-api-key";

    // Builder API Key endpoints
    public static final String CREATE_BUILDER_API_KEY = "/auth/builder-api-key";
    public static final String GET_BUILDER_API_KEYS = "/auth/builder-api-key";
    public static final String REVOKE_BUILDER_API_KEY = "/auth/builder-api-key";

    // Heartbeat
    public static final String POST_HEARTBEAT = "/v1/heartbeats";

    // Trading endpoints
    public static final String TRADES = "/data/trades";
    public static final String GET_ORDER_BOOK = "/book";
    public static final String GET_ORDER_BOOKS = "/books";
    public static final String GET_ORDER = "/data/order/";
    public static final String ORDERS = "/data/orders";
    public static final String PRE_MIGRATION_ORDERS = "/data/pre-migration-orders";
    public static final String POST_ORDER = "/order";
    public static final String POST_ORDERS = "/orders";
    public static final String CANCEL = "/order";
    public static final String CANCEL_ORDERS = "/orders";
    public static final String CANCEL_ALL = "/cancel-all";
    public static final String CANCEL_MARKET_ORDERS = "/cancel-market-orders";
    
    // Market data endpoints
    public static final String MID_POINT = "/midpoint";
    public static final String MID_POINTS = "/midpoints";
    public static final String PRICE = "/price";
    public static final String GET_PRICES = "/prices";
    public static final String GET_SPREAD = "/spread";
    public static final String GET_SPREADS = "/spreads";
    public static final String GET_LAST_TRADE_PRICE = "/last-trade-price";
    public static final String GET_LAST_TRADES_PRICES = "/last-trades-prices";
    
    // Notification endpoints
    public static final String GET_NOTIFICATIONS = "/notifications";
    public static final String DROP_NOTIFICATIONS = "/notifications";
    
    // Balance and allowance endpoints
    public static final String GET_BALANCE_ALLOWANCE = "/balance-allowance";
    public static final String UPDATE_BALANCE_ALLOWANCE = "/balance-allowance/update";
    
    // Order scoring endpoints
    public static final String IS_ORDER_SCORING = "/order-scoring";
    public static final String ARE_ORDERS_SCORING = "/orders-scoring";
    
    // Market configuration endpoints
    public static final String GET_TICK_SIZE = "/tick-size";
    public static final String GET_NEG_RISK = "/neg-risk";
    public static final String GET_FEE_RATE = "/fee-rate";
    
    // Market information endpoints
    public static final String GET_SAMPLING_SIMPLIFIED_MARKETS = "/sampling-simplified-markets";
    public static final String GET_SAMPLING_MARKETS = "/sampling-markets";
    public static final String GET_SIMPLIFIED_MARKETS = "/simplified-markets";
    public static final String GET_MARKETS = "/markets";
    public static final String GET_MARKET = "/markets/";
    public static final String GET_MARKET_BY_TOKEN = "/markets-by-token/";
    public static final String GET_CLOB_MARKET = "/clob-markets/";
    public static final String GET_MARKET_TRADES_EVENTS = "/markets/live-activity/";

    // Prices history
    public static final String GET_PRICES_HISTORY = "/prices-history";

    // Builder endpoints
    public static final String GET_BUILDER_TRADES = "/builder/trades";
    public static final String GET_BUILDER_FEE_RATE = "/fees/builder-fees/";

    // Rewards endpoints
    public static final String GET_EARNINGS_FOR_USER_FOR_DAY = "/rewards/user";
    public static final String GET_TOTAL_EARNINGS_FOR_USER_FOR_DAY = "/rewards/user/total";
    public static final String GET_LIQUIDITY_REWARD_PERCENTAGES = "/rewards/user/percentages";
    public static final String GET_REWARDS_MARKETS_CURRENT = "/rewards/markets/current";
    public static final String GET_REWARDS_MARKETS = "/rewards/markets/";
    public static final String GET_REWARDS_EARNINGS_PERCENTAGES = "/rewards/user/markets";

    // RFQ Endpoints
    public static final String CREATE_RFQ_REQUEST = "/rfq/request";
    public static final String CANCEL_RFQ_REQUEST = "/rfq/request";
    public static final String GET_RFQ_REQUESTS = "/rfq/data/requests";
    public static final String CREATE_RFQ_QUOTE = "/rfq/quote";
    public static final String CANCEL_RFQ_QUOTE = "/rfq/quote";
    public static final String GET_RFQ_QUOTES = "/rfq/data/quotes";
    public static final String GET_RFQ_REQUESTER_QUOTES = "/rfq/data/requester/quotes";
    public static final String GET_RFQ_QUOTER_QUOTES = "/rfq/data/quoter/quotes";
    public static final String GET_RFQ_BEST_QUOTE = "/rfq/data/best-quote";
    public static final String RFQ_REQUESTS_ACCEPT = "/rfq/request/accept";
    public static final String RFQ_QUOTE_APPROVE = "/rfq/quote/approve";
    public static final String RFQ_CONFIG = "/rfq/config";
}
