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
