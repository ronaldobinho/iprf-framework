package io.iprf.domain;

import java.util.List;
import java.util.Objects;

/**
 * The outcome of evaluating one layer.
 *
 * <p>Latency is captured in microseconds rather than milliseconds. The in-path
 * budget is 50 ms total with a 5 ms allocation for Layer 1 — at millisecond
 * granularity most layer measurements would round to zero and the per-layer
 * budget would be unverifiable.
 *
 * @param layer         which layer produced this
 * @param status        how the evaluation concluded
 * @param contribution  this layer's contribution to the composite score, in {@code [0, 1]}
 * @param riskFactors   the individual rule contributions, in evaluation order
 * @param latencyMicros measured wall-clock duration of this layer
 * @param stateVersion  version of the pre-computed state read, or {@code null} if none
 */
public record LayerResult(
        ControlLayer layer,
        LayerStatus status,
        double contribution,
        List<RiskFactor> riskFactors,
        long latencyMicros,
        String stateVersion) {

    public LayerResult {
        Objects.requireNonNull(layer, "layer");
        Objects.requireNonNull(status, "status");
        riskFactors = riskFactors == null ? List.of() : List.copyOf(riskFactors);
        if (contribution < 0.0 || contribution > 1.0) {
            throw new IllegalArgumentException(
                    "contribution must be within [0, 1], was " + contribution);
        }
    }

    /**
     * A layer that could not evaluate on complete input. Contributes a neutral
     * zero <em>with</em> an explicit reason code — the framework's rule is that
     * degradation is never silent and never favorable.
     */
    public static LayerResult degraded(ControlLayer layer, ReasonCode reason, long latencyMicros) {
        return new LayerResult(
                layer,
                LayerStatus.DEGRADED,
                0.0,
                List.of(RiskFactor.degraded(reason, layer)),
                latencyMicros,
                null);
    }

    /** A layer cut off at its latency budget. */
    public static LayerResult timedOut(ControlLayer layer, long latencyMicros) {
        return new LayerResult(
                layer,
                LayerStatus.TIMED_OUT,
                0.0,
                List.of(RiskFactor.degraded(ReasonCode.LAYER_TIMEOUT, layer)),
                latencyMicros,
                null);
    }

    /**
     * Returns a copy stamped with an authoritative measured duration.
     *
     * <p>Evaluators do not time themselves — the pipeline times them. A layer
     * reporting its own latency would exclude dispatch overhead and would be
     * trusted to be honest about a number its own budget is judged on.
     */
    public LayerResult withLatencyMicros(long micros) {
        return new LayerResult(layer, status, contribution, riskFactors, micros, stateVersion);
    }

    /** True when this layer's contribution was produced on incomplete input. */
    public boolean isDegraded() {
        return status == LayerStatus.DEGRADED || status == LayerStatus.TIMED_OUT;
    }

    public double latencyMillis() {
        return latencyMicros / 1000.0;
    }
}
