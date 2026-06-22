package com.polymarket.clob.positions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Locale;

public final class PositionUtils {
    private static final Logger logger = LogManager.getLogger(PositionUtils.class);

    public static final int CONDITIONAL_TOKEN_DECIMALS = 6;
    private static final BigInteger DEFAULT_GAS_LIMIT = BigInteger.valueOf(10_000_000L);
    private static final BigInteger TOKEN_DECIMALS_MULTIPLIER = BigInteger.TEN.pow(CONDITIONAL_TOKEN_DECIMALS);

    private PositionUtils() {
    }

    public static BigInteger toTokenUnits(double amount) {
        return BigDecimal.valueOf(amount)
                .multiply(new BigDecimal(TOKEN_DECIMALS_MULTIPLIER))
                .setScale(0, RoundingMode.DOWN)
                .toBigIntegerExact();
    }

    public static byte[] toBytes32(String input) {
        if (input == null) {
            throw new IllegalArgumentException("conditionId cannot be null");
        }

        String normalized = input.toLowerCase(Locale.ROOT).startsWith("0x") ? input.substring(2) : input;
        if (normalized.length() > 64) {
            throw new IllegalArgumentException("conditionId must be <= 32 bytes hex");
        }
        if ((normalized.length() & 1) == 1) {
            normalized = "0" + normalized;
        }

        byte[] decoded = Numeric.hexStringToByteArray("0x" + normalized);
        if (decoded.length > 32) {
            throw new IllegalArgumentException("conditionId must be <= 32 bytes");
        }

        byte[] out = new byte[32];
        System.arraycopy(decoded, 0, out, 32 - decoded.length, decoded.length);
        return out;
    }

    public static String sendTransaction(Web3j web3j, Credentials credentials, long chainId,
                                         String toAddress, String encodedFunction) throws Exception {
        TransactionManager txManager = new RawTransactionManager(web3j, credentials, chainId);
        EthSendTransaction response = txManager.sendTransaction(
                resolveGasPrice(web3j),
                DEFAULT_GAS_LIMIT,
                toAddress,
                encodedFunction,
                BigInteger.ZERO
        );
        if (response.hasError()) {
            throw new RuntimeException("Transaction error: " + response.getError().getMessage());
        }
        return response.getTransactionHash();
    }

    public static BigInteger resolveGasPrice(Web3j web3j) {
        try {
            BigInteger networkGasPrice = web3j.ethGasPrice().send().getGasPrice();
            if (networkGasPrice != null && networkGasPrice.signum() > 0) {
                return networkGasPrice;
            }
        } catch (Exception e) {
            logger.warn("Failed to fetch network gas price, using default provider gas price", e);
        }
        return DefaultGasProvider.GAS_PRICE;
    }
}

