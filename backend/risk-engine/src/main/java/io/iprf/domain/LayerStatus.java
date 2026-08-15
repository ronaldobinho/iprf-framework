package io.iprf.domain;

/**
 * How a layer's evaluation concluded.
 *
 * <p>Anything other than {@link #EVALUATED} is recorded on the decision, so the
 * audit trail always shows whether a decision was made on complete input.
 */
public enum LayerStatus {

    /** Completed normally on complete input. */
    EVALUATED,

    /**
     * Ran successfully, but there was nothing to evaluate against — the layer
     * looked and genuinely nothing is known.
     *
     * <p>Distinct from both {@link #EVALUATED} and {@link #DEGRADED}, and the
     * distinction carries weight. Treating it as {@code EVALUATED} would put the
     * layer in the score denominator contributing zero, which makes silence act
     * as evidence of safety — the exact thing this framework says it must not be.
     * Treating it as {@code DEGRADED} would report almost every decision as
     * degraded, since most counterparties are unknown, and destroy the usefulness
     * of that metric.
     *
     * <p>So it is its own state: excluded from scoring, not counted as a
     * failure, and always accompanied by a reason code.
     */
    NO_DATA,

    /**
     * Completed, but on missing or stale input. Contributes a neutral value plus
     * an explicit reason code — never a silent zero and never a favorable
     * default.
     */
    DEGRADED,

    /** Cut off at its latency budget. Partial result recorded. */
    TIMED_OUT,

    /** Not run — layer disabled by configuration, or not applicable to this transaction. */
    SKIPPED
}
