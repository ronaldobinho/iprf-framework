package io.iprf.domain;

/**
 * Whether a layer runs on the payment path.
 *
 * <p>There is no third category and no exception process. A layer that cannot
 * satisfy every in-path condition — deterministic, bounded, pre-computed reads
 * only, explicit degradation — is {@link #ASYNC}.
 */
public enum PathClassification {

    /** Evaluated during authorization, inside the latency budget. Can block a payment. */
    IN_PATH,

    /** Evaluated off the payment path. Feeds future decisions. Never blocks a payment. */
    ASYNC
}
