package io.iprf.domain;

/**
 * The stable vocabulary of reasons a rule fired or a layer degraded.
 *
 * <p><b>This enum is a published interface.</b> Reason codes appear in persisted
 * audit records, in API responses, and in the TypeScript simulator that must
 * produce identical codes for identical inputs (verified by a parity test in
 * CI). Codes are therefore added, never renamed or repurposed — a historical
 * audit record must keep meaning the same thing years later.
 *
 * <p>Codes are grouped by the layer that emits them. Degradation codes are
 * separated from detection codes because they mean fundamentally different
 * things: a detection code says something about the transaction, a degradation
 * code says something about the system's ability to evaluate it.
 */
public enum ReasonCode {

    // --- Layer 1: Identity & Account Posture — detections -------------------

    ACCOUNT_AGE_LOW(ControlLayer.IDENTITY_POSTURE,
            "Account was opened recently"),
    VERIFICATION_INCOMPLETE(ControlLayer.IDENTITY_POSTURE,
            "Account identity verification is below the expected tier"),
    DEVICE_UNKNOWN(ControlLayer.IDENTITY_POSTURE,
            "Payment initiated from a device not previously seen for this account"),
    ACCOUNT_RESTRICTED(ControlLayer.IDENTITY_POSTURE,
            "Account carries an active restriction or prior confirmed-fraud marker"),

    // --- Layer 1 — degradations ---------------------------------------------

    IDENTITY_PROFILE_MISSING(ControlLayer.IDENTITY_POSTURE,
            "No pre-loaded profile exists for this account; treated as maximum uncertainty"),
    IDENTITY_PROFILE_STALE(ControlLayer.IDENTITY_POSTURE,
            "Account profile is older than the configured freshness window"),

    // --- Layer 2: Real-Time Behavioral Scoring — detections -----------------

    AMOUNT_DEVIATION_HIGH(ControlLayer.BEHAVIORAL_SCORING,
            "Amount is far outside this payer's established range"),
    COUNTERPARTY_NEW(ControlLayer.BEHAVIORAL_SCORING,
            "First payment from this payer to this counterparty"),
    TIMING_UNUSUAL_HOUR(ControlLayer.BEHAVIORAL_SCORING,
            "Payment initiated outside this payer's typical active hours"),
    VELOCITY_WINDOW_EXCEEDED(ControlLayer.BEHAVIORAL_SCORING,
            "Payment count in the rolling window exceeds this payer's normal rate"),
    CHANNEL_UNUSUAL(ControlLayer.BEHAVIORAL_SCORING,
            "Payment initiated on a channel this payer does not normally use"),

    // --- Layer 2 — degradations ---------------------------------------------

    BASELINE_INSUFFICIENT(ControlLayer.BEHAVIORAL_SCORING,
            "Payer has too little history for deviation rules; conservative absolute "
                    + "thresholds applied instead"),

    // --- Layer 3: Counterparty & Network Signals — detections ---------------

    COUNTERPARTY_RISK_TIER_ELEVATED(ControlLayer.COUNTERPARTY_NETWORK,
            "Receiving account carries an elevated pre-computed risk tier"),
    COUNTERPARTY_FAN_IN_PATTERN(ControlLayer.COUNTERPARTY_NETWORK,
            "Receiving account shows a fan-in pattern consistent with mule activity"),
    COUNTERPARTY_REPORTED_TYPOLOGY(ControlLayer.COUNTERPARTY_NETWORK,
            "Receiving account has a previously reported fraud typology"),

    // --- Layer 3 — degradations ---------------------------------------------

    NETWORK_STATE_ABSENT(ControlLayer.COUNTERPARTY_NETWORK,
            "No pre-computed state exists for this counterparty"),
    NETWORK_STATE_STALE(ControlLayer.COUNTERPARTY_NETWORK,
            "Counterparty state is older than the configured TTL"),
    NETWORK_STATE_UNAVAILABLE(ControlLayer.COUNTERPARTY_NETWORK,
            "The risk state store could not be reached; layer degraded whole"),

    // --- Pipeline-level ------------------------------------------------------

    /**
     * Emitted when a layer exceeds its latency budget. The partial result is
     * kept and the pipeline continues; a timeout never silently becomes an
     * {@link Decision#ALLOW}.
     */
    LAYER_TIMEOUT(null,
            "Layer exceeded its latency budget and was cut off"),

    NO_SIGNAL(null,
            "No rule produced a contribution");

    private final ControlLayer layer;
    private final String explanation;

    ReasonCode(ControlLayer layer, String explanation) {
        this.layer = layer;
        this.explanation = explanation;
    }

    /** The layer that emits this code, or {@code null} for pipeline-level codes. */
    public ControlLayer layer() {
        return layer;
    }

    /** Human-readable explanation, carried into API responses and audit records. */
    public String explanation() {
        return explanation;
    }
}
