package io.iprf.transaction.api;

import io.iprf.domain.ControlLayer;
import io.iprf.domain.Decision;
import io.iprf.domain.EvaluationResult;
import io.iprf.domain.LayerResult;
import io.iprf.domain.LayerStatus;
import io.iprf.domain.PathClassification;
import io.iprf.domain.RiskFactor;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;

/**
 * The decision, with everything needed to explain it.
 *
 * <p>Nothing here is optional or omitted for brevity. A response that reported
 * only the decision and the score would be a black box with extra steps — the
 * factors, the per-layer detail and the rule versions are the point.
 */
@Schema(description = "An explainable decision. All figures derive from SYNTHETIC DATA.")
public record EvaluateTransactionResponse(

        String transactionId,
        String correlationId,

        @Schema(description = "ALLOW, REVIEW or DECLINE", example = "REVIEW")
        Decision decision,

        @Schema(description = "Composite risk score in [0, 1]", example = "0.42")
        double riskScore,

        @Schema(description = "Measured in-path pipeline duration in milliseconds", example = "0.412")
        double latencyMs,

        @Schema(description = "Measured in-path pipeline duration in microseconds", example = "412")
        long latencyMicros,

        @Schema(description = "Every contributing factor, highest contribution first")
        List<RiskFactorResponse> riskFactors,

        @Schema(description = "Per-layer detail, keyed by layer")
        Map<String, LayerResultResponse> layerResults,

        String explanation,

        @Schema(description = "True when any layer evaluated on incomplete input")
        boolean degraded,

        String frameworkVersion,
        Instant evaluatedAt) {

    public static EvaluateTransactionResponse from(EvaluationResult result) {
        Map<String, LayerResultResponse> layers = new LinkedHashMap<>();
        result.layerResults().entrySet().stream()
                .sorted(Comparator.comparingInt(e -> e.getKey().number()))
                .forEach(e -> layers.put(e.getKey().name(), LayerResultResponse.from(e.getValue())));

        return new EvaluateTransactionResponse(
                result.transactionId(),
                result.correlationId(),
                result.decision(),
                round(result.riskScore()),
                result.totalLatencyMillis(),
                result.totalLatencyMicros(),
                result.riskFactors().stream()
                        .sorted(Comparator.comparingDouble(RiskFactor::contribution).reversed())
                        .map(RiskFactorResponse::from)
                        .toList(),
                layers,
                result.explanation(),
                result.isDegraded(),
                result.frameworkVersion(),
                result.evaluatedAt());
    }

    /** One rule's contribution. */
    @Schema(description = "A single rule's contribution to the composite score")
    public record RiskFactorResponse(
            String code,
            String layer,
            int layerNumber,
            double contribution,
            String ruleId,
            String ruleVersion,
            String explanation) {

        static RiskFactorResponse from(RiskFactor factor) {
            return new RiskFactorResponse(
                    factor.code().name(),
                    factor.layer() == null ? null : factor.layer().name(),
                    factor.layer() == null ? 0 : factor.layer().number(),
                    round(factor.contribution()),
                    factor.ruleId(),
                    factor.ruleVersion(),
                    factor.explanation());
        }
    }

    /** One layer's outcome. */
    @Schema(description = "The outcome of evaluating one control layer")
    public record LayerResultResponse(
            int layerNumber,
            String layerName,

            @Schema(description = "IN_PATH or ASYNC — a fixed property of the layer")
            PathClassification path,

            LayerStatus status,
            double contribution,
            double latencyMs,
            long latencyMicros,
            List<String> reasonCodes,

            @Schema(description = "Version of the pre-computed state read, if any")
            String stateVersion) {

        static LayerResultResponse from(LayerResult result) {
            ControlLayer layer = result.layer();
            return new LayerResultResponse(
                    layer.number(),
                    layer.displayName(),
                    layer.path(),
                    result.status(),
                    round(result.contribution()),
                    result.latencyMillis(),
                    result.latencyMicros(),
                    result.riskFactors().stream().map(f -> f.code().name()).distinct().toList(),
                    result.stateVersion());
        }
    }

    private static double round(double value) {
        return Math.round(value * 10_000.0) / 10_000.0;
    }
}
