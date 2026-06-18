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
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Merges equal amounts of YES and NO conditional tokens back into USDC collateral.
 *
 * <p>Merging is useful for:
 * <ul>
 *   <li>Reducing exposure – convert a balanced YES+NO position into cash</li>
 *   <li>Exiting a market – close out a position by merging both sides</li>
 *   <li>Freeing up capital – reclaim USDC locked in conditional tokens</li>
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
public class PositionMerger {

    private static final Logger logger = LogManager.getLogger(PositionMerger.class);

    /** USDC uses 6 decimal places; conditional tokens inherit the same precision. */
    public static final int CONDITIONAL_TOKEN_DECIMALS = 6;

    private static final BigInteger DEFAULT_GAS_LIMIT = BigInteger.valueOf(500_000L);

    private final Web3j web3j;
    private final Credentials credentials;
    private final ContractConfig contracts;
    private final long chainId;

    /**
     * Creates a new {@code PositionMerger}.
     *
     * @param rpcUrl     JSON-RPC endpoint URL (e.g. a Polygon mainnet RPC)
     * @param privateKey hex private key (with or without {@code 0x} prefix)
     * @param chainId    chain ID: {@code 137} for Polygon mainnet, {@code 80002} for Amoy testnet
     */
    public PositionMerger(String rpcUrl, String privateKey, int chainId) {
        this.web3j = Web3j.build(new HttpService(rpcUrl));
        this.credentials = Credentials.create(privateKey.startsWith("0x") ? privateKey : "0x" + privateKey);
        this.contracts = Config.getContractConfig(chainId);
        this.chainId = chainId;
    }

    /**
     * Merges YES + NO tokens back into USDC via the <b>NegRiskAdapter</b>.
     *
     * <p>Use this for neg-risk (multi-outcome) markets. You must hold at least
     * {@code amount} of <em>both</em> YES and NO tokens; the contract burns them
     * and returns the equivalent USDC.
     *
     * @param conditionId 32-byte condition ID of the market, hex-encoded with {@code 0x} prefix
     * @param amount      token amount to merge in human-readable units (e.g. {@code 10.0} for 10 tokens)
     * @return transaction hash of the submitted transaction
     * @throws Exception if the RPC call fails or the transaction is rejected
     */
    public String mergeNegRisk(String conditionId, double amount) throws Exception {
        BigInteger amountUnits = toTokenUnits(amount);
        logger.info("mergePositions (neg-risk): conditionId={} amount={}", conditionId, amount);

        Function function = new Function(
                "mergePositions",
                Arrays.asList(
                        new Bytes32(Numeric.hexStringToByteArray(conditionId)),
                        new Uint256(amountUnits)
                ),
                Collections.emptyList()
        );

        String txHash = sendTransaction(contracts.getNegRiskAdapter(), FunctionEncoder.encode(function));
        logger.info("mergePositions (neg-risk) submitted txHash={}", txHash);
        return txHash;
    }

    /**
     * Merges YES + NO tokens back into USDC via the <b>ConditionalTokens</b> contract.
     *
     * <p>Use this for standard binary markets. The partition {@code [1, 2]} must match
     * the one used when the position was originally split.
     *
     * @param conditionId 32-byte condition ID of the market, hex-encoded with {@code 0x} prefix
     * @param amount      token amount to merge in human-readable units (e.g. {@code 10.0} for 10 tokens)
     * @return transaction hash of the submitted transaction
     * @throws Exception if the RPC call fails or the transaction is rejected
     */
    public String mergeStandard(String conditionId, double amount) throws Exception {
        BigInteger amountUnits = toTokenUnits(amount);
        logger.info("mergePositions (standard CTF): conditionId={} amount={}", conditionId, amount);

        // partition [1, 2] = the two outcome index sets for a binary market
        List<Uint256> partition = Arrays.asList(new Uint256(BigInteger.ONE), new Uint256(BigInteger.TWO));

        Function function = new Function(
                "mergePositions",
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
        logger.info("mergePositions (standard CTF) submitted txHash={}", txHash);
        return txHash;
    }

    /**
     * Convenience dispatcher: calls {@link #mergeNegRisk} or {@link #mergeStandard} based on
     * the {@code isNegRisk} flag.
     *
     * @param conditionId 32-byte condition ID, hex-encoded with {@code 0x} prefix
     * @param amount      token amount in human-readable units
     * @param isNegRisk   {@code true} to use the NegRiskAdapter; {@code false} for the CTF contract
     * @return transaction hash
     * @throws Exception if the transaction fails
     */
    public String merge(String conditionId, double amount, boolean isNegRisk) throws Exception {
        return isNegRisk ? mergeNegRisk(conditionId, amount) : mergeStandard(conditionId, amount);
    }

    /**
     * Shuts down the underlying {@link Web3j} instance, releasing its thread pool.
     * Call this when the merger is no longer needed.
     */
    public void shutdown() {
        web3j.shutdown();
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private BigInteger toTokenUnits(double amount) {
        BigInteger multiplier = BigInteger.TEN.pow(CONDITIONAL_TOKEN_DECIMALS);
        return BigDecimal.valueOf(amount)
                .multiply(new BigDecimal(multiplier))
                .setScale(0, RoundingMode.DOWN)
                .toBigIntegerExact();
    }

    private String sendTransaction(String toAddress, String encodedFunction) throws Exception {
        TransactionManager txManager = new RawTransactionManager(web3j, credentials, chainId);

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
