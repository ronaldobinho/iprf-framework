package io.iprf.network;

import io.iprf.domain.ControlLayer;
import io.iprf.domain.LayerResult;
import io.iprf.domain.LayerStatus;
import io.iprf.domain.NetworkFlag;
import io.iprf.domain.ReasonCode;
import io.iprf.domain.RiskFactor;
import io.iprf.domain.RuleDefinition;
import io.iprf.domain.RuleVersion;
import io.iprf.domain.Transaction;
import io.iprf.engine.LayerEvaluator;
import io.iprf.engine.RuleProperties;
import io.iprf.state.CounterpartyRiskState;
import io.iprf.state.RiskStateStore;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Layer 3 — Counterparty &amp; Network Signals.
 *
 * <p>Answers "what do we already know about where this money is going?" without
 * performing any lookup that could be slow. The questions this layer wants to
 * ask are naturally graph questions — how many distinct payers have sent to this
 * account this hour, is it two hops from a confirmed mule — and those are
 * genuinely expensive. The resolution is not to skip them: it is to run them in
 * Layer 5 and read the result here, at the cost of one pre-computed lookup.
 *
 * <p>This layer is designed to be droppable. It contributes the most valuable
 * signal when state is available and contributes nothing harmful when it is not.
 * A control that takes down the payment path when its datastore is unavailable
 * has converted a fraud control into an availability incident.
 */
@Component
public class CounterpartyNetworkEvaluator implements LayerEvaluator {

    static final RuleDefinition RISK_TIER = new RuleDefinition(
            "L3.RISK_TIER", ControlLayer.COUNTERPARTY_NETWORK,
            ReasonCode.COUNTERPARTY_RISK_TIER_ELEVATED, RuleVersion.INITIAL, 1.0, true,
            "Fires when the receiving account carries an adverse pre-computed risk tier");

    static final RuleDefinition FAN_IN = new RuleDefinition(
            "L3.FAN_IN", ControlLayer.COUNTERPARTY_NETWORK,
            ReasonCode.COUNTERPARTY_FAN_IN_PATTERN, RuleVersion.INITIAL, 1.0, true,
            "Fires when Layer 5 has flagged the receiver with a fan-in collection pattern");

    static final RuleDefinition REPORTED_TYPOLOGY = new RuleDefinition(
            "L3.REPORTED_TYPOLOGY", ControlLayer.COUNTERPARTY_NETWORK,
            ReasonCode.COUNTERPARTY_REPORTED_TYPOLOGY, RuleVersion.INITIAL, 1.0, true,
            "Fires when a fraud typology has been reported against the receiver");

    private final RiskStateStore riskStateStore;
    private final RuleProperties properties;
    private final Clock clock;

    public CounterpartyNetworkEvaluator(
            RiskStateStore riskStateStore, RuleProperties properties, Clock clock) {
        this.riskStateStore = riskStateStore;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public ControlLayer layer() {
        return ControlLayer.COUNTERPARTY_NETWORK;
    }

    @Override
    public LayerResult evaluate(Transaction transaction) {
        // Losing the store degrades this layer whole. Layers 1-2 carry the
        // decision, and DecisionPolicy deliberately does not escalate on a
        // Layer 3 degradation — otherwise every Redis blip becomes a review queue.
        if (!riskStateStore.isAvailable()) {
            return LayerResult.degraded(layer(), ReasonCode.NETWORK_STATE_UNAVAILABLE, 0);
        }

        Optional<CounterpartyRiskState> found =
                riskStateStore.findByCounterpartyId(transaction.payeeAccountId());

        // The layer looked and genuinely nothing is known. NO_DATA rather than
        // DEGRADED (it did not fail) and rather than EVALUATED (it has nothing
        // to say). The distinction is load-bearing: an EVALUATED zero would sit
        // in the score denominator and dilute the composite, making an unknown
        // counterparty act as evidence of safety.
        if (found.isEmpty()) {
            return new LayerResult(
                    layer(), LayerStatus.NO_DATA, 0.0,
                    List.of(RiskFactor.degraded(ReasonCode.NETWORK_STATE_ABSENT, layer())),
                    0, null);
        }

        CounterpartyRiskState state = found.get();
        RuleProperties.NetworkRules rules = properties.network();
        Instant now = clock.instant();

        if (state.isStaleAt(now, Duration.ofMinutes(rules.stateTtlMinutes()))) {
            // Explicitly not a blocking refresh and explicitly not a silent zero.
            return new LayerResult(
                    layer(), LayerStatus.DEGRADED, 0.0,
                    List.of(RiskFactor.degraded(ReasonCode.NETWORK_STATE_STALE, layer())),
                    0, state.versionLabel());
        }

        List<RiskFactor> factors = new ArrayList<>();

        double tierWeight = rules.weightFor(state.tier());
        if (state.tier().isAdverse() && tierWeight > 0.0) {
            factors.add(factor(RISK_TIER, tierWeight,
                    "Receiving account is classified " + state.tier()
                            + " in pre-computed risk state (version " + state.version() + ")"));
        }

        if (state.flags().contains(NetworkFlag.FAN_IN)) {
            factors.add(factor(FAN_IN, rules.fanInWeight(),
                    state.distinctPayers() > 0
                            ? "Receiving account has taken payments from " + state.distinctPayers()
                                    + " distinct payers in the aggregation window"
                            : ReasonCode.COUNTERPARTY_FAN_IN_PATTERN.explanation()));
        }

        if (!state.reportedTypologies().isEmpty()) {
            factors.add(factor(REPORTED_TYPOLOGY, rules.reportedTypologyWeight(),
                    "Reported typology against the receiving account: "
                            + String.join(", ", state.reportedTypologies())));
        }

        return new LayerResult(
                layer(),
                LayerStatus.EVALUATED,
                sumContributions(factors),
                factors,
                0,
                // Recorded on the decision and in the audit trail: reconstructing
                // a decision requires knowing what the system believed then, not
                // what it believes now.
                state.versionLabel());
    }

    private static RiskFactor factor(RuleDefinition rule, double contribution, String explanation) {
        return new RiskFactor(
                rule.reasonCode(), rule.layer(), contribution,
                rule.id(), rule.version().value(), explanation);
    }

    static double sumContributions(List<RiskFactor> factors) {
        return Math.min(1.0, factors.stream().mapToDouble(RiskFactor::contribution).sum());
    }

}
