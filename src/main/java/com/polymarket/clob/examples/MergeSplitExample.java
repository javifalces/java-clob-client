package com.polymarket.clob.examples;

import com.polymarket.clob.ClobClient;
import com.polymarket.clob.positions.PositionMerger;
import com.polymarket.clob.positions.PositionSplitter;

import static com.polymarket.clob.Constants.POLYGON;

/**
 * Demonstrates how to split and merge Polymarket conditional token positions
 * using a shared {@link ClobClient} so that credentials are created only once.
 *
 * <p><b>Splitting</b> converts USDC collateral into equal amounts of YES and NO tokens.
 * This is useful for:
 * <ul>
 *   <li>Providing liquidity on both sides of a market</li>
 *   <li>Obtaining tokens to place sell orders on the CLOB</li>
 * </ul>
 *
 * <p><b>Merging</b> burns equal amounts of YES and NO tokens and returns the equivalent
 * USDC collateral. This is useful for:
 * <ul>
 *   <li>Reducing exposure or exiting a market cleanly</li>
 *   <li>Freeing up capital locked in conditional tokens</li>
 * </ul>
 *
 * <p>Required environment variables:
 * <ul>
 *   <li>{@code POLYMARKET_PK}       – hex-encoded private key</li>
 *   <li>{@code POLYMARKET_RPC_URL}  – JSON-RPC endpoint for Polygon</li>
 *   <li>{@code CONDITION_ID}        – 32-byte condition ID of the target market</li>
 *   <li>{@code IS_NEG_RISK}         – {@code "true"} for neg-risk markets, {@code "false"} otherwise</li>
 * </ul>
 *
 * <p>Based on:
 * <a href="https://github.com/Polymarket/ts-merge-split-positions">ts-merge-split-positions</a>
 * and <a href="https://docs.polymarket.com/market-makers/inventory">Polymarket inventory docs</a>.
 */
public class MergeSplitExample {

    public static void main(String[] args) {
        String privateKey = System.getenv("POLYMARKET_PK");
        String rpcUrl = System.getenv("POLYMARKET_RPC_URL");
        String conditionId = System.getenv("CONDITION_ID");
        boolean isNegRisk = "true".equalsIgnoreCase(System.getenv("IS_NEG_RISK"));

        if (privateKey == null || rpcUrl == null || conditionId == null) {
            System.err.println("Required environment variables: POLYMARKET_PK, POLYMARKET_RPC_URL, CONDITION_ID");
            System.exit(1);
        }

        // Amount of USDC / tokens to split or merge (human-readable, e.g. 10.0 = $10)
        double amount = 10.0;

        // -------------------------------------------------------------------------
        // Build a ClobClient once – credentials and chain ID are shared with the
        // PositionSplitter and PositionMerger via the fromClobClient() factory.
        // -------------------------------------------------------------------------
        ClobClient clobClient = new ClobClient("https://clob.polymarket.com", POLYGON, privateKey);
        System.out.println("ClobClient address: " + clobClient.getAddress());

        // -------------------------------------------------------------------------
        // Split: USDC → YES + NO tokens (using ClobClient credentials)
        // -------------------------------------------------------------------------
        PositionSplitter splitter = PositionSplitter.fromClobClient(clobClient, rpcUrl);
        try {
            System.out.println("=== Split Position ===");
            System.out.printf("Splitting %.2f USDC into YES+NO tokens (neg-risk=%b)%n", amount, isNegRisk);
            System.out.println("Condition ID: " + conditionId);

            String splitTxHash = splitter.split(conditionId, amount, isNegRisk);
            System.out.println("Split submitted! Transaction hash: " + splitTxHash);
        } catch (Exception e) {
            System.err.println("Split failed: " + e.getMessage());
        } finally {
            splitter.shutdown();
        }

        // -------------------------------------------------------------------------
        // Merge: YES + NO tokens → USDC (using ClobClient credentials)
        // -------------------------------------------------------------------------
        PositionMerger merger = PositionMerger.fromClobClient(clobClient, rpcUrl);
        try {
            System.out.println("\n=== Merge Position ===");
            System.out.printf("Merging %.2f YES+NO tokens back to USDC (neg-risk=%b)%n", amount, isNegRisk);
            System.out.println("Condition ID: " + conditionId);

            String mergeTxHash = merger.merge(conditionId, amount, isNegRisk);
            System.out.println("Merge submitted! Transaction hash: " + mergeTxHash);
        } catch (Exception e) {
            System.err.println("Merge failed: " + e.getMessage());
        } finally {
            merger.shutdown();
        }
    }
}
