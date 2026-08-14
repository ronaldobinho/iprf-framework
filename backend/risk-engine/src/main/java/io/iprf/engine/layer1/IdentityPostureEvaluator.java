package io.iprf.engine.layer1;

import io.iprf.domain.ControlLayer;
import io.iprf.domain.LayerResult;
import io.iprf.domain.LayerStatus;
import io.iprf.domain.ReasonCode;
import io.iprf.domain.RiskFactor;
import io.iprf.domain.RuleDefinition;
import io.iprf.domain.RuleVersion;
import io.iprf.domain.Transaction;
import io.iprf.engine.LayerEvaluator;
import io.iprf.engine.RuleProperties;
import io.iprf.state.AccountProfile;
import io.iprf.state.AccountProfileStore;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Layer 1 — Identity &amp; Account Posture.
 *
 * <p>Asks whether the account, device and channel are in a posture consistent
 * with initiating this payment at all. Runs first because it is the cheapest
 * layer and a decisive answer here avoids the cost of everything downstream.
 *
 * <p>Reads only the pre-computed {@link AccountProfileStore}. No query, no call
 * out, no clock-dependent branching beyond account age.
 */
@Component
public class IdentityPostureEvaluator implements LayerEvaluator {

    static final RuleDefinition ACCOUNT_AGE = new RuleDefinition(
            "L1.ACCOUNT_AGE", ControlLayer.IDENTITY_POSTURE, ReasonCode.ACCOUNT_AGE_LOW,
            RuleVersion.INITIAL, 1.0, true,
            "Fires when the account is younger than the configured threshold");

    static final RuleDefinition VERIFICATION = new RuleDefinition(
            "L1.VERIFICATION", ControlLayer.IDENTITY_POSTURE, ReasonCode.VERIFICATION_INCOMPLETE,
            RuleVersion.INITIAL, 1.0, true,
            "Fires when identity verification is incomplete");

    static final RuleDefinition DEVICE = new RuleDefinition(
            "L1.DEVICE", ControlLayer.IDENTITY_POSTURE, ReasonCode.DEVICE_UNKNOWN,
            RuleVersion.INITIAL, 1.0, true,
            "Fires when the payment comes from a device not previously seen for this account");

    static final RuleDefinition RESTRICTION = new RuleDefinition(
            "L1.RESTRICTION", ControlLayer.IDENTITY_POSTURE, ReasonCode.ACCOUNT_RESTRICTED,
            RuleVersion.INITIAL, 1.0, true,
            "Fires when the account carries an active restriction or confirmed-fraud marker");

    private final AccountProfileStore profileStore;
    private final RuleProperties properties;
    private final Clock clock;

    public IdentityPostureEvaluator(
            AccountProfileStore profileStore, RuleProperties properties, Clock clock) {
        this.profileStore = profileStore;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public ControlLayer layer() {
        return ControlLayer.IDENTITY_POSTURE;
    }

    @Override
    public LayerResult evaluate(Transaction transaction) {
        Optional<AccountProfile> found = profileStore.findByAccountId(transaction.payerAccountId());

        // An unknown payer is an operational gap, not evidence of fraud. It
        // degrades rather than declining outright — but it degrades loudly, and
        // DecisionPolicy escalates the resulting ALLOW to REVIEW.
        if (found.isEmpty()) {
            return LayerResult.degraded(layer(), ReasonCode.IDENTITY_PROFILE_MISSING, 0);
        }

        AccountProfile profile = found.get();
        RuleProperties.IdentityRules rules = properties.identity();
        Instant now = clock.instant();

        if (isStale(profile, rules, now)) {
            return LayerResult.degraded(layer(), ReasonCode.IDENTITY_PROFILE_STALE, 0);
        }

        List<RiskFactor> factors = new ArrayList<>();

        if (profile.openedAt() != null
                && Duration.between(profile.openedAt(), now).toDays() < rules.newAccountDays()) {
            long ageDays = Duration.between(profile.openedAt(), now).toDays();
            factors.add(factor(ACCOUNT_AGE, rules.newAccountWeight(),
                    "Account is " + ageDays + " days old, below the " + rules.newAccountDays()
                            + "-day threshold"));
        }

        if (!profile.verified()) {
            factors.add(factor(VERIFICATION, rules.unverifiedWeight(),
                    ReasonCode.VERIFICATION_INCOMPLETE.explanation()));
        }

        if (!profile.isKnownDevice(transaction.deviceId())) {
            factors.add(factor(DEVICE, rules.unknownDeviceWeight(),
                    transaction.deviceId() == null
                            ? "No device identifier was supplied for this channel"
                            : "Device has not been seen for this account before"));
        }

        if (profile.restricted()) {
            factors.add(factor(RESTRICTION, rules.restrictedWeight(),
                    ReasonCode.ACCOUNT_RESTRICTED.explanation()));
        }

        return new LayerResult(
                layer(),
                LayerStatus.EVALUATED,
                sumContributions(factors),
                factors,
                0,
                null);
    }

    private static boolean isStale(AccountProfile profile, RuleProperties.IdentityRules rules, Instant now) {
        return profile.computedAt() != null
                && Duration.between(profile.computedAt(), now).toHours() >= rules.profileMaxAgeHours();
    }

    private static RiskFactor factor(RuleDefinition rule, double contribution, String explanation) {
        return new RiskFactor(
                rule.reasonCode(), rule.layer(), contribution,
                rule.id(), rule.version().value(), explanation);
    }

    /**
     * Contributions are additive and clamped rather than averaged: three
     * simultaneous posture problems are worse than one, and averaging would let
     * additional problems dilute the signal.
     */
    static double sumContributions(List<RiskFactor> factors) {
        double sum = factors.stream().mapToDouble(RiskFactor::contribution).sum();
        return Math.min(1.0, sum);
    }
}
