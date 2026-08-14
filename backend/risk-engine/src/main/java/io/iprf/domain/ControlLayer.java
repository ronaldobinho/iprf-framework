package io.iprf.domain;

/**
 * The five control layers, each with a fixed path classification.
 *
 * <p>The classification is a property of the layer, not of the deployment or the
 * configuration. It cannot be varied per environment — that is the entire point
 * of deciding it at design time. See {@code docs/framework/methodology.md}.
 */
public enum ControlLayer {

    IDENTITY_POSTURE(1, "Identity & Account Posture", PathClassification.IN_PATH),

    BEHAVIORAL_SCORING(2, "Real-Time Behavioral Scoring", PathClassification.IN_PATH),

    /**
     * In-path, but under a stricter restriction than layers 1-2: it may read
     * only pre-computed counterparty risk state, never a synchronous lookup of
     * any kind.
     */
    COUNTERPARTY_NETWORK(3, "Counterparty & Network Signals", PathClassification.IN_PATH),

    EXTERNAL_ENRICHMENT(4, "External Enrichment", PathClassification.ASYNC),

    POST_SETTLEMENT(5, "Post-Settlement Analysis", PathClassification.ASYNC);

    private final int number;
    private final String displayName;
    private final PathClassification path;

    ControlLayer(int number, String displayName, PathClassification path) {
        this.number = number;
        this.displayName = displayName;
        this.path = path;
    }

    public int number() {
        return number;
    }

    public String displayName() {
        return displayName;
    }

    public PathClassification path() {
        return path;
    }

    /** True when this layer participates in the transaction authorization path. */
    public boolean isInPath() {
        return path == PathClassification.IN_PATH;
    }
}
