package io.iprf.engine.layer2;

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
import io.iprf.state.VelocityCounterStore;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Layer 2 — Real-Time Behavioral Scoring.
 *
 * <p>Detects deviation from <em>this payer's</em> established baseline. Absolute
 * thresholds are a weak control: they are trivially learned and evaded, and they
 * generate false positives against legitimately high-value customers. Deviation
 * from a personal baseline is harder to evade because the attacker does not know
 * the baseline.
 *
 * <p>Individual signals here are weak and each one alone is a reliable
 * false-positive generator — a first payment to a new counterparty is what
 * happens every time someone pays a new landlord. Composition is what turns them
 * into a usable signal.
 */
@Component
public class BehavioralScoringEvaluator implements LayerEvaluator {

    private static final Duration VELOCITY_WINDOW = Duration.ofHours(1);

    static final RuleDefinition AMOUNT_DEVIATION = new RuleDefinition(
            "L2.AMOUNT_DEVIATION", ControlLayer.BEHAVIORAL_SCORING, ReasonCode.AMOUNT_DEVIATION_HIGH,
            RuleVersion.INITIAL, 1.0, true,
            "Fires when the amount exceeds the payer's mean by the configured number of "
                    + "standard deviations");

    static final RuleDefinition NEW_COUNTERPARTY = new RuleDefinition(
            "L2.NEW_COUNTERPARTY", ControlLayer.BEHAVIORAL_SCORING, ReasonCode.COUNTERPARTY_NEW,
            RuleVersion.INITIAL, 1.0, true,
            "Fires on the payer's first payment to this counterparty");

    static final RuleDefinition UNUSUAL_HOUR = new RuleDefinition(
            "L2.UNUSUAL_HOUR", ControlLayer.BEHAVIORAL_SCORING, ReasonCode.TIMING_UNUSUAL_HOUR,
            RuleVersion.INITIAL, 1.0, true,
            "Fires outside the payer's typical active hours");

    static final RuleDefinition VELOCITY = new RuleDefinition(
            "L2.VELOCITY", ControlLayer.BEHAVIORAL_SCORING, ReasonCode.VELOCITY_WINDOW_EXCEEDED,
            RuleVersion.INITIAL, 1.0, true,
            "Fires when payments in the rolling window exceed the configured rate");

    static final RuleDefinition CHANNEL_SWITCH = new RuleDefinition(
            "L2.CHANNEL_SWITCH", ControlLayer.BEHAVIORAL_SCORING, ReasonCode.CHANNEL_UNUSUAL,
            RuleVersion.INITIAL, 1.0, true,
            "Fires when the payment uses a channel this payer does not normally use");

    private final AccountProfileStore profileStore;
    private final VelocityCounterStore velocityStore;
    private final RuleProperties properties;
    private final Clock clock;

    public BehavioralScoringEvaluator(
            AccountProfileStore profileStore,
            VelocityCounterStore velocityStore,
            RuleProperties properties,
            Clock clock) {
        this.profileStore = profileStore;
        this.velocityStore = velocityStore;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public ControlLayer layer() {
        return ControlLayer.BEHAVIORAL_SCORING;
    }

    @Override
    public LayerResult evaluate(Transaction transaction) {
        Instant now = clock.instant();
        RuleProperties.BehavioralRules rules = properties.behavioral();

        Optional<AccountProfile> found = profileStore.findByAccountId(transaction.payerAccountId());
        if (found.isEmpty()) {
            recordVelocity(transaction, now);
            return LayerResult.degraded(layer(), ReasonCode.BASELINE_INSUFFICIENT, 0);
        }
        AccountProfile profile = found.get();

        List<RiskFactor> factors = new ArrayList<>();
        boolean baselineUsable = profile.hasSufficientBaseline(rules.minimumBaselineTransactions());

        // --- amount ---------------------------------------------------------
        if (baselineUsable) {
            evaluateAmountDeviation(transaction, profile, rules).ifPresent(factors::add);
        } else {
            // New accounts are exactly where mule activity concentrates, so an
            // unusable baseline must not silently resolve to "low risk". Fall
            // back to a conservative absolute threshold and say so.
            factors.add(new RiskFactor(
                    ReasonCode.BASELINE_INSUFFICIENT, layer(), 0.0,
                    "L2.BASELINE_FALLBACK", RuleVersion.INITIAL.value(),
                    "Payer has " + profile.transactionCount() + " transactions, below the "
                            + rules.minimumBaselineTransactions()
                            + " required for deviation rules; absolute threshold applied"));
            if (transaction.amount().compareTo(rules.fallbackAbsoluteAmount()) >= 0) {
                factors.add(factor(AMOUNT_DEVIATION, rules.amountDeviationWeight(),
                        "Amount " + transaction.amount() + " " + transaction.currency()
                                + " meets the absolute threshold of " + rules.fallbackAbsoluteAmount()
                                + " applied to payers without a baseline"));
            }
        }

        // --- counterparty ---------------------------------------------------
        if (!transaction.isSelfTransfer() && !profile.isKnownCounterparty(transaction.payeeAccountId())) {
            factors.add(factor(NEW_COUNTERPARTY, rules.newCounterpartyWeight(),
                    ReasonCode.COUNTERPARTY_NEW.explanation()));
        }

        // --- timing ---------------------------------------------------------
        if (isOutsideActiveHours(transaction, profile)) {
            int hour = transaction.initiatedAt().atZone(ZoneOffset.UTC).getHour();
            factors.add(factor(UNUSUAL_HOUR, rules.unusualHourWeight(),
                    "Initiated at " + hour + ":00 UTC, outside this payer's active window of "
                            + profile.activeHourStart() + ":00-" + profile.activeHourEnd() + ":00"));
        }

        // --- velocity -------------------------------------------------------
        int priorInWindow = velocityStore.countWithin(
                transaction.payerAccountId(), VELOCITY_WINDOW, now);
        if (priorInWindow >= rules.velocityMaxPerHour()) {
            factors.add(factor(VELOCITY, rules.velocityWeight(),
                    priorInWindow + " payments in the preceding hour, at or above the configured "
                            + "maximum of " + rules.velocityMaxPerHour()));
        }

        // --- channel --------------------------------------------------------
        if (!profile.typicalChannels().isEmpty()
                && !profile.typicalChannels().contains(transaction.channel())) {
            factors.add(factor(CHANNEL_SWITCH, rules.channelSwitchWeight(),
                    "Channel " + transaction.channel() + " is not among this payer's typical channels"));
        }

        // Counted before recording, so the current payment does not inflate its
        // own velocity reading. This is the one write the payment path performs;
        // it is a bounded counter increment, not a query.
        recordVelocity(transaction, now);

        return new LayerResult(
                layer(),
                LayerStatus.EVALUATED,
                sumContributions(factors),
                factors,
                0,
                null);
    }

    private Optional<RiskFactor> evaluateAmountDeviation(
            Transaction transaction, AccountProfile profile, RuleProperties.BehavioralRules rules) {

        BigDecimal stdDev = profile.baselineAmountStdDev() == null
                ? BigDecimal.ZERO : profile.baselineAmountStdDev();
        BigDecimal threshold = profile.baselineAmountMean()
                .add(stdDev.multiply(BigDecimal.valueOf(rules.amountDeviationSigmas())));

        if (transaction.amount().compareTo(threshold) < 0) {
            return Optional.empty();
        }

        String sigmas = stdDev.signum() == 0
                ? "n/a (zero variance)"
                : transaction.amount().subtract(profile.baselineAmountMean())
                        .divide(stdDev, 1, RoundingMode.HALF_UP).toPlainString();

        return Optional.of(factor(AMOUNT_DEVIATION, rules.amountDeviationWeight(),
                "Amount " + transaction.amount() + " " + transaction.currency()
                        + " is " + sigmas + " standard deviations above this payer's mean of "
                        + profile.baselineAmountMean()));
    }

    /**
     * Active-hour comparison is performed in UTC.
     *
     * <p>A production deployment should compare against the payer's local time —
     * a payer in a different timezone from the institution would otherwise be
     * flagged for transacting at breakfast. The profile does not yet carry a
     * timezone; this is a known limitation rather than a modelling choice.
     */
    private static boolean isOutsideActiveHours(Transaction transaction, AccountProfile profile) {
        int start = profile.activeHourStart();
        int end = profile.activeHourEnd();
        if (start == end) {
            return false;
        }
        int hour = transaction.initiatedAt().atZone(ZoneOffset.UTC).getHour();
        return start <= end
                ? hour < start || hour > end
                : hour < start && hour > end;  // window wraps past midnight
    }

    private void recordVelocity(Transaction transaction, Instant now) {
        velocityStore.record(transaction.payerAccountId(), now);
    }

    private static RiskFactor factor(RuleDefinition rule, double contribution, String explanation) {
        return new RiskFactor(
                rule.reasonCode(), rule.layer(), contribution,
                rule.id(), rule.version().value(), explanation);
    }

    static double sumContributions(List<RiskFactor> factors) {
        double sum = factors.stream().mapToDouble(RiskFactor::contribution).sum();
        return Math.min(1.0, sum);
    }
}
