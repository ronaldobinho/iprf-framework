package io.iprf.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The complete, explainable outcome of evaluating a transaction.
 *
 * <p>Everything needed to reconstruct why this decision was made is carried
 * here: the layers that ran, the rules that fired with their versions, the
 * pre-computed state versions that were read, and the measured latency. A
 * decision that cannot be reproduced cannot be audited.
 *
 * @param transactionId      the evaluated transaction
 * @param correlationId      request correlation identifier, propagated to async consumers
 * @param decision           ALLOW / REVIEW / DECLINE
 * @param riskScore          composite score in {@code [0, 1]}
 * @param riskFactors        every contributing factor, flattened across layers
 * @param layerResults       per-layer detail, keyed by layer
 * @param explanation        human-readable summary of the decision
 * @param frameworkVersion   version of the rule set that produced it
 * @param evaluatedAt        when evaluation completed
 * @param totalLatencyMicros measured duration of the whole in-path pipeline
 */
public record EvaluationResult(
        String transactionId,
        String correlationId,
        Decision decision,
        double riskScore,
        List<RiskFactor> riskFactors,
        Map<ControlLayer, LayerResult> layerResults,
        String explanation,
        String frameworkVersion,
        Instant evaluatedAt,
        long totalLatencyMicros) {

    public EvaluationResult {
        Objects.requireNonNull(transactionId, "transactionId");
        Objects.requireNonNull(decision, "decision");
        Objects.requireNonNull(frameworkVersion, "frameworkVersion");
        Objects.requireNonNull(evaluatedAt, "evaluatedAt");
        riskFactors = riskFactors == null ? List.of() : List.copyOf(riskFactors);
        layerResults = layerResults == null ? Map.of() : Map.copyOf(layerResults);
        if (riskScore < 0.0 || riskScore > 1.0) {
            throw new IllegalArgumentException(
                    "riskScore must be within [0, 1], was " + riskScore);
        }
    }

    /**
     * True when any layer evaluated on incomplete input.
     *
     * <p>Surfaced in the API response and tracked as a metric: an institution
     * needs to know what share of its decisions were made on degraded state,
     * because that share is invisible in the decision distribution alone.
     */
    public boolean isDegraded() {
        return layerResults.values().stream().anyMatch(LayerResult::isDegraded);
    }

    public double totalLatencyMillis() {
        return totalLatencyMicros / 1000.0;
    }
}
