package io.iprf.domain;

/**
 * Pre-computed risk classification of a receiving account.
 *
 * <p>A tier is the compressed output of expensive asynchronous analysis — fan-in
 * detection, external intelligence, confirmed-fraud feedback. Layer 3 reads it
 * in-path at the cost of a single lookup, which is the whole mechanism by which
 * cross-transactional analysis reaches a real-time decision.
 *
 * <p>Ordered least to most severe. {@link #UNKNOWN} is deliberately first and
 * deliberately not {@code LOW}: absence of information is not evidence of
 * safety, and conflating the two is how a system with an empty state store
 * reports every counterparty as clean.
 */
public enum CounterpartyRiskTier {

    /** No pre-computed state exists for this counterparty. Not the same as low risk. */
    UNKNOWN,

    /** Analysed, nothing adverse found. */
    LOW,

    /** Some adverse signal — unusual inbound patterns, thin history with high volume. */
    ELEVATED,

    /** Strong adverse signal, typically a detected typology. */
    HIGH,

    /** Confirmed fraudulent or mule account. */
    CONFIRMED;

    public boolean isAdverse() {
        return this == ELEVATED || this == HIGH || this == CONFIRMED;
    }
}
