package io.iprf.synthetic;

/**
 * The generated scenarios, and their ground truth.
 *
 * <p>The two middle scenarios are the ones that make measurement honest. A
 * generator producing only obviously-normal and obviously-fraudulent traffic
 * would report a near-perfect detection rate and a near-zero false-positive
 * rate, and both numbers would be artifacts of the generator rather than
 * measurements of the rule set.
 */
public enum Scenario {

    /** Ordinary payment matching the payer's baseline. */
    NORMAL(false),

    /**
     * Legitimate, but genuinely unusual — paying a new landlord, a holiday
     * purchase, a transfer made from a hotel at 3am. <b>This is where false
     * positives come from</b>, and a control set is judged on how many of these
     * it lets through.
     */
    UNUSUAL_LEGITIMATE(false),

    /** Fraud with strong signals: large, new counterparty, unknown device, odd hour. */
    FRAUD_OVERT(true),

    /**
     * Fraud deliberately shaped to resemble the payer's normal behaviour —
     * modest amount, plausible hour. <b>This is where false negatives come
     * from</b>, and it is what an attacker who has studied the controls
     * produces.
     */
    FRAUD_SUBTLE(true),

    /** Account takeover: unknown device, channel switch, rapid sequence. */
    FRAUD_TAKEOVER(true);

    private final boolean fraudulent;

    Scenario(boolean fraudulent) {
        this.fraudulent = fraudulent;
    }

    /** Ground truth. Assigned at generation time, which is what makes it trustworthy. */
    public boolean isFraudulent() {
        return fraudulent;
    }
}
