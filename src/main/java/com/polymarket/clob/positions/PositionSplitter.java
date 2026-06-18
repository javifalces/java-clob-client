package com.polymarket.clob.positions;

import com.polymarket.clob.config.Config;
import com.polymarket.clob.model.ContractConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Splits USDC collateral into YES and NO conditional tokens for a given market.
 *
 * <p>Splitting is useful for:
 * <ul>
 *   <li>Reducing directional exposure by holding both sides</li>
 *   <li>Providing liquidity on the CLOB without a net position</li>
 *   <li>Obtaining tokens to sell on the order book</li>
 * </ul>
 *
 * <p>Supports both standard binary (CTF) markets and neg-risk markets via the
 * NegRiskAdapter contract.
 *
 * <p>Reference implementations:
 * <ul>
 *   <li><a href="https://github.com/Polymarket/ts-merge-split-positions">ts-merge-split-positions (TypeScript)</a></li>
 *   <li><a href="https://docs.polymarket.com/market-makers/inventory">Polymarket inventory docs</a></li>
 * </ul>
 */
public class PositionSplitter {

    private static final Logger logger = LogManager.getLogger(PositionSplitter.class);

    /** USDC uses 6 decimal places; conditional tokens inherit the same precision. */
    public static final int CONDITIONAL_TOKEN_DECIMALS = 6;

    private static final BigInteger DEFAULT_GAS_LIMIT = BigInteger.valueOf(500_000L);

    private final Web3j web3j;
    private final Credentials credentials;
    private final ContractConfig contracts;
    private final long chainId;

    /**
     * Creates a new {@code PositionSplitter}.
     *
     * @param rpcUrl     JSON-RPC endpoint URL (e.g. a Polygon mainnet RPC)
     * @param privateKey hex private key (with or without {@code 0x} prefix)
     * @param chainId    chain ID: {@code 137} for Polygon mainnet, {@code 80002} for Amoy testnet
     */
    public PositionSplitter(String rpcUrl, String privateKey, int chainId) {
        this.web3j = Web3j.build(new HttpService(rpcUrl));
        this.credentials = Credentials.create(privateKey.startsWith("0x") ? privateKey : "0x" + privateKey);
        this.contracts = Config.getContractConfig(chainId);
        this.chainId = chainId;
    }

    /**
     * Splits USDC collateral into YES + NO tokens via the <b>NegRiskAdapter</b>.
     *
     * <p>Use this for neg-risk (multi-outcome) markets. After the call succeeds you
     * will hold equal amounts of YES and NO tokens for the condition.
     *
     * @param conditionId 32-byte condition ID of the market, hex-encoded with {@code 0x} prefix
     * @param amount      USDC amount to split in human-readable units (e.g. {@code 10.0} for $10)
     * @return transaction hash of the submitted transaction
     * @throws Exception if the RPC call fails or the transaction is rejected
     */
    public String splitNegRisk(String conditionId, double amount) throws Exception {
        BigInteger amountUnits = toTokenUnits(amount);
        logger.info("splitPosition (neg-risk): conditionId={} amount={}", conditionId, amount);

        Function function = new Function(
                "splitPosition",
                Arrays.asList(
                        new Bytes32(Numeric.hexStringToByteArray(conditionId)),
                        new Uint256(amountUnits)
                ),
                Collections.emptyList()
        );

        String txHash = sendTransaction(contracts.getNegRiskAdapter(), FunctionEncoder.encode(function));
        logger.info("splitPosition (neg-risk) submitted txHash={}", txHash);
        return txHash;
    }

    /**
     * Splits USDC collateral into YES + NO tokens via the <b>ConditionalTokens</b> contract.
     *
     * <p>Use this for standard binary markets. The partition {@code [1, 2]} represents the
     * two outcome index sets (YES = bit 0, NO = bit 1).
     *
     * @param conditionId 32-byte condition ID of the market, hex-encoded with {@code 0x} prefix
     * @param amount      USDC amount to split in human-readable units (e.g. {@code 10.0} for $10)
     * @return transaction hash of the submitted transaction
     * @throws Exception if the RPC call fails or the transaction is rejected
     */
    public String splitStandard(String conditionId, double amount) throws Exception {
        BigInteger amountUnits = toTokenUnits(amount);
        logger.info("splitPosition (standard CTF): conditionId={} amount={}", conditionId, amount);

        // partition [1, 2] = the two outcome index sets for a binary market
        List<Uint256> partition = Arrays.asList(new Uint256(BigInteger.ONE), new Uint256(BigInteger.TWO));

        Function function = new Function(
                "splitPosition",
                Arrays.asList(
                        new org.web3j.abi.datatypes.Address(contracts.getCollateral()),
                        new Bytes32(new byte[32]),   // parentCollectionId = bytes32(0)
                        new Bytes32(Numeric.hexStringToByteArray(conditionId)),
                        new DynamicArray<>(Uint256.class, partition),
                        new Uint256(amountUnits)
                ),
                Collections.emptyList()
        );

        String txHash = sendTransaction(contracts.getConditionalTokens(), FunctionEncoder.encode(function));
        logger.info("splitPosition (standard CTF) submitted txHash={}", txHash);
        return txHash;
    }

    /**
     * Convenience dispatcher: calls {@link #splitNegRisk} or {@link #splitStandard} based on
     * the {@code isNegRisk} flag.
     *
     * @param conditionId 32-byte condition ID, hex-encoded with {@code 0x} prefix
     * @param amount      USDC amount in human-readable units
     * @param isNegRisk   {@code true} to use the NegRiskAdapter; {@code false} for the CTF contract
     * @return transaction hash
     * @throws Exception if the transaction fails
     */
    public String split(String conditionId, double amount, boolean isNegRisk) throws Exception {
        return isNegRisk ? splitNegRisk(conditionId, amount) : splitStandard(conditionId, amount);
    }

    /**
     * Shuts down the underlying {@link Web3j} instance, releasing its thread pool.
     * Call this when the splitter is no longer needed.
     */
    public void shutdown() {
        web3j.shutdown();
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private BigInteger toTokenUnits(double amount) {
        BigInteger multiplier = BigInteger.TEN.pow(CONDITIONAL_TOKEN_DECIMALS);
        return BigDecimal.valueOf(amount).multiply(new BigDecimal(multiplier)).toBigIntegerExact();
    }

    private String sendTransaction(String toAddress, String encodedFunction) throws Exception {
        TransactionManager txManager = new RawTransactionManager(web3j, credentials, chainId);

        EthGetTransactionCount nonceFetch = web3j
                .ethGetTransactionCount(credentials.getAddress(), DefaultBlockParameterName.LATEST)
                .send();

        if (nonceFetch.hasError()) {
            throw new RuntimeException("Failed to fetch nonce: " + nonceFetch.getError().getMessage());
        }

        EthSendTransaction response = txManager.sendTransaction(
                DefaultGasProvider.GAS_PRICE,
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
}
