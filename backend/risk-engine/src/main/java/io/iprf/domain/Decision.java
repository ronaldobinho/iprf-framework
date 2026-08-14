package io.iprf.domain;

/**
 * The outcome of evaluating a transaction.
 *
 * <p>Three outcomes rather than two. {@link #REVIEW} exists because both major
 * instant-payment rails provide a hold-and-review mechanism — Pix grants a
 * fraud-suspicion authorization window of 30 minutes (08:00-20:00 Brasilia,
 * business days) or 60 minutes otherwise, and FedNow provides "accept without
 * posting" — and because without it every uncertain transaction has to be
 * forced into a binary at authorization time.
 *
 * <p>See {@code docs/framework/false-positive-model.md}.
 */
public enum Decision {

    /** Proceed on the real-time path. */
    ALLOW,

    /**
     * Hold for asynchronous assessment within the window the rail permits.
     * Counted as a false positive when the transaction was legitimate: a
     * legitimate payment that did not complete on the real-time path is a
     * degraded outcome regardless of which non-allow bucket it landed in.
     */
    REVIEW,

    /** Reject the payment. */
    DECLINE
}
