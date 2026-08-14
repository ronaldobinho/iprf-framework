package io.iprf.engine;

import io.iprf.domain.ControlLayer;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Rule configuration, bound from {@code application-rules.yml}.
 *
 * <p>Every number that determines an outcome lives here rather than in code.
 * Changing the score at which a payment is held for review must not require a
 * recompilation — see {@code docs/framework/assessment-model.md}, control
 * RTD-04.
 *
 * <p>This file is also a published interface: Phase 5 generates the TypeScript
 * simulator's constants from it at build time so the browser simulator and the
 * Java engine cannot silently diverge.
 *
 * <p>Validation is performed in the compact constructors and fails at startup.
 * A malformed rule configuration must stop the application rather than produce
 * quietly wrong decisions.
 *
 * @param layerWeights relative weight of each in-path layer in the composite score
 * @param decision     score boundaries between ALLOW, REVIEW and DECLINE
 * @param identity     Layer 1 rule parameters
 * @param behavioral   Layer 2 rule parameters
 */
@ConfigurationProperties(prefix = "iprf.rules")
public record RuleProperties(
        Map<ControlLayer, Double> layerWeights,
        DecisionThresholds decision,
        IdentityRules identity,
        BehavioralRules behavioral) {

    public RuleProperties {
        // Checked before copying: EnumMap cannot be constructed from an empty
        // map (it has no key type to infer) and would throw its own exception
        // ahead of this validation, hiding the real problem.
        if (layerWeights == null || layerWeights.isEmpty()) {
            throw new IllegalStateException(
                    "iprf.rules.layer-weights is empty — the engine would score every "
                            + "transaction at zero. Check that application-rules.yml is imported.");
        }
        layerWeights = new EnumMap<>(layerWeights);
        layerWeights.forEach((layer, weight) -> {
            if (weight == null || weight < 0.0) {
                throw new IllegalStateException(
                        "iprf.rules.layer-weights." + layer + " must not be negative, was " + weight);
            }
        });
        requireSection(decision, "decision");
        requireSection(identity, "identity");
        requireSection(behavioral, "behavioral");
    }

    private static void requireSection(Object section, String name) {
        if (section == null) {
            throw new IllegalStateException("iprf.rules." + name + " is required");
        }
    }

    /**
     * Score boundaries.
     *
     * <p>A score strictly below {@code reviewThreshold} is ALLOW; at or above
     * {@code declineThreshold} is DECLINE; between them is REVIEW.
     */
    public record DecisionThresholds(double reviewThreshold, double declineThreshold) {

        public DecisionThresholds {
            requireUnitInterval(reviewThreshold, "decision.review-threshold");
            requireUnitInterval(declineThreshold, "decision.decline-threshold");
            if (reviewThreshold > declineThreshold) {
                throw new IllegalStateException(
                        "review-threshold (" + reviewThreshold + ") must not exceed decline-threshold ("
                                + declineThreshold + ") — an inverted configuration would make "
                                + "REVIEW unreachable and route uncertain payments straight to DECLINE");
            }
        }
    }

    /**
     * Layer 1 parameters.
     *
     * @param newAccountDays        accounts younger than this are treated as new
     * @param newAccountWeight      contribution when the account is new
     * @param unverifiedWeight      contribution when verification is incomplete
     * @param unknownDeviceWeight   contribution when the device has not been seen before
     * @param restrictedWeight      contribution when the account carries a restriction
     * @param profileMaxAgeHours    profiles older than this are treated as stale
     */
    public record IdentityRules(
            int newAccountDays,
            double newAccountWeight,
            double unverifiedWeight,
            double unknownDeviceWeight,
            double restrictedWeight,
            long profileMaxAgeHours) {

        public IdentityRules {
            requireUnitInterval(newAccountWeight, "identity.new-account-weight");
            requireUnitInterval(unverifiedWeight, "identity.unverified-weight");
            requireUnitInterval(unknownDeviceWeight, "identity.unknown-device-weight");
            requireUnitInterval(restrictedWeight, "identity.restricted-weight");
            if (newAccountDays < 0) {
                throw new IllegalStateException("identity.new-account-days must not be negative");
            }
            if (profileMaxAgeHours <= 0) {
                throw new IllegalStateException("identity.profile-max-age-hours must be positive");
            }
        }
    }

    /**
     * Layer 2 parameters.
     *
     * @param amountDeviationSigmas       standard deviations above the payer's mean before the
     *                                    amount rule fires
     * @param amountDeviationWeight       contribution when it fires
     * @param newCounterpartyWeight       contribution for a first-ever counterparty
     * @param unusualHourWeight           contribution outside the payer's active hours
     * @param velocityMaxPerHour          payments per hour before the velocity rule fires
     * @param velocityWeight              contribution when it fires
     * @param channelSwitchWeight         contribution for an atypical channel
     * @param minimumBaselineTransactions history required before deviation rules are meaningful
     * @param fallbackAbsoluteAmount      absolute threshold used when the baseline is insufficient
     */
    public record BehavioralRules(
            double amountDeviationSigmas,
            double amountDeviationWeight,
            double newCounterpartyWeight,
            double unusualHourWeight,
            int velocityMaxPerHour,
            double velocityWeight,
            double channelSwitchWeight,
            int minimumBaselineTransactions,
            BigDecimal fallbackAbsoluteAmount) {

        public BehavioralRules {
            requireUnitInterval(amountDeviationWeight, "behavioral.amount-deviation-weight");
            requireUnitInterval(newCounterpartyWeight, "behavioral.new-counterparty-weight");
            requireUnitInterval(unusualHourWeight, "behavioral.unusual-hour-weight");
            requireUnitInterval(velocityWeight, "behavioral.velocity-weight");
            requireUnitInterval(channelSwitchWeight, "behavioral.channel-switch-weight");
            if (amountDeviationSigmas <= 0) {
                throw new IllegalStateException("behavioral.amount-deviation-sigmas must be positive");
            }
            if (velocityMaxPerHour <= 0) {
                throw new IllegalStateException("behavioral.velocity-max-per-hour must be positive");
            }
            if (minimumBaselineTransactions < 0) {
                throw new IllegalStateException(
                        "behavioral.minimum-baseline-transactions must not be negative");
            }
            if (fallbackAbsoluteAmount == null || fallbackAbsoluteAmount.signum() <= 0) {
                throw new IllegalStateException(
                        "behavioral.fallback-absolute-amount must be positive — without it, a payer "
                                + "with no baseline would face no amount control at all");
            }
        }
    }

    private static void requireUnitInterval(double value, String name) {
        if (value < 0.0 || value > 1.0) {
            throw new IllegalStateException(
                    "iprf.rules." + name + " must be within [0, 1], was " + value);
        }
    }
}
