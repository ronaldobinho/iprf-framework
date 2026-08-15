package io.iprf.domain;

/**
 * Behavioural patterns detected against a counterparty by Layer 5.
 *
 * <p>Each flag is the durable record of a pattern that no single transaction
 * could reveal. They are written asynchronously after settlement and read
 * in-path on subsequent transactions.
 *
 * <p>Like {@link ReasonCode}, this is a published vocabulary: flags are
 * persisted in risk state and appear in audit records, so they are added rather
 * than renamed.
 */
public enum NetworkFlag {

    /** Many unrelated payers to this receiver in a short window — mule collection. */
    FAN_IN,

    /** This account paying many new receivers in a short window — dispersal. */
    FAN_OUT,

    /** Repeated amounts just below a known threshold. */
    STRUCTURING,

    /** Settled-transaction rate far above the account's established pattern. */
    VELOCITY_BURST,

    /** Inbound and outbound totals nearly match with short dwell time — pass-through. */
    RAPID_PASS_THROUGH
}
