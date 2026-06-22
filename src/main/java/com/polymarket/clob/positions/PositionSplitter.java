package com.polymarket.clob.positions;

import com.polymarket.clob.ClobClient;
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

import org.web3j.abi.datatypes.Address;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static com.polymarket.clob.positions.PositionUtils.*;

/**
 * Splits USDC collateral into YES+NO conditional tokens.
 */
public class PositionSplitter {
    private static final Logger logger = LogManager.getLogger(PositionSplitter.class);

    private final Web3j web3j;
    private final Credentials credentials;
    private final ContractConfig contracts;
    private final long chainId;

    public PositionSplitter(String rpcUrl, String privateKey, int chainId) {
        this.web3j = Web3j.build(new HttpService(rpcUrl));
        this.credentials = Credentials.create(privateKey.startsWith("0x") ? privateKey : "0x" + privateKey);
        this.contracts = Config.getContractConfig(chainId);
        this.chainId = chainId;
    }

    public PositionSplitter(String rpcUrl, Credentials credentials, int chainId) {
        this.web3j = Web3j.build(new HttpService(rpcUrl));
        this.credentials = credentials;
        this.contracts = Config.getContractConfig(chainId);
        this.chainId = chainId;
    }

    public static PositionSplitter fromClobClient(ClobClient clobClient, String rpcUrl) {
        if (clobClient.getSigner() == null) {
            throw new IllegalArgumentException("ClobClient must have a signer to create a PositionSplitter");
        }
        return new PositionSplitter(rpcUrl, clobClient.getSigner().getCredentials(), clobClient.getChainId());
    }
    public String splitNegRisk(String conditionId, double amount) throws Exception {
        BigInteger amountUnits = toTokenUnits(amount);
        logger.info("splitPosition (neg-risk): conditionId={} amount={}", conditionId, amount);
        Function function = new Function(
                "splitPosition",
                Arrays.asList(new Bytes32(toBytes32(conditionId)), new Uint256(amountUnits)),
                Collections.emptyList()
        );


        String txHash = sendTransaction(web3j, credentials, chainId, contracts.getNegRiskAdapter(), FunctionEncoder.encode(function));
        logger.info("splitPosition (neg-risk) submitted txHash={}", txHash);
        return txHash;
    }
    public String splitStandard(String conditionId, double amount) throws Exception {
        BigInteger amountUnits = toTokenUnits(amount);
        logger.info("splitPosition (standard CTF): conditionId={} amount={}", conditionId, amount);
        List<Uint256> partition = Arrays.asList(new Uint256(BigInteger.ONE), new Uint256(BigInteger.TWO));
        Function function = new Function(
                "splitPosition",
                Arrays.asList(
                        new Address(contracts.getCollateral()),
                        new Bytes32(new byte[32]),
                        new Bytes32(toBytes32(conditionId)),
                        new DynamicArray<>(Uint256.class, partition),
                        new Uint256(amountUnits)
                ),
                Collections.emptyList()
        );
        String txHash = sendTransaction(web3j, credentials, chainId, contracts.getConditionalTokens(), FunctionEncoder.encode(function));
        logger.info("splitPosition (standard CTF) submitted txHash={}", txHash);
        return txHash;
    }
    public String split(String conditionId, double amount, boolean isNegRisk) throws Exception {
        return isNegRisk ? splitNegRisk(conditionId, amount) : splitStandard(conditionId, amount);
    }

    public void shutdown() {
        web3j.shutdown(); }


}
