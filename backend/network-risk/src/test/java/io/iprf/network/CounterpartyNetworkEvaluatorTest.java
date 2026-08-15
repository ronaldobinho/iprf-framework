package io.iprf.network;

import static org.assertj.core.api.Assertions.assertThat;

import io.iprf.domain.Channel;
import io.iprf.domain.ControlLayer;
import io.iprf.domain.CounterpartyRiskTier;
import io.iprf.domain.LayerResult;
import io.iprf.domain.LayerStatus;
import io.iprf.domain.NetworkFlag;
import io.iprf.domain.Rail;
import io.iprf.domain.ReasonCode;
import io.iprf.domain.RiskFactor;
import io.iprf.domain.Transaction;
import io.iprf.engine.RuleProperties;
import io.iprf.state.CounterpartyRiskState;
import io.iprf.state.InMemoryRiskStateStore;
import io.iprf.state.RiskStateStore;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CounterpartyNetworkEvaluatorTest {

    private static final Instant NOW = Instant.parse("2026-08-15T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final String PAYEE = "acct-payee";

    private InMemoryRiskStateStore store;
    private CounterpartyNetworkEvaluator evaluator;

    @BeforeEach
    void setUp() {
        store = new InMemoryRiskStateStore(CLOCK);
        evaluator = new CounterpartyNetworkEvaluator(store, properties(), CLOCK);
    }

    @Nested
    @DisplayName("degradation is explicit and distinguishable")
    class Degradation {

        @Test
        @DisplayName("no state is NO_DATA — not a failure, and not evidence of safety")
        void absentStateIsNoData() {
            LayerResult result = evaluator.evaluate(transaction());

            assertThat(result.status()).isEqualTo(LayerStatus.NO_DATA);
            assertThat(codes(result)).containsExactly(ReasonCode.NETWORK_STATE_ABSENT);
            assertThat(result.contribution()).isZero();
            // Not a failure: reporting every unknown counterparty as degraded
            // would make that metric meaningless.
            assertThat(result.isDegraded()).isFalse();
            // But excluded from scoring: an EVALUATED zero would sit in the
            // denominator and dilute the composite, letting an unknown
            // counterparty act as evidence of safety.
            assertThat(result.contributesToScore()).isFalse();
        }

        @Test
        @DisplayName("an unreachable store degrades with UNAVAILABLE — a different finding")
        void unavailableStoreDegrades() {
            CounterpartyNetworkEvaluator offline = new CounterpartyNetworkEvaluator(
                    unavailableStore(), properties(), CLOCK);

            LayerResult result = offline.evaluate(transaction());

            // "No state for this counterparty" and "the store is down" are
            // different problems and must not share a reason code.
            assertThat(codes(result)).containsExactly(ReasonCode.NETWORK_STATE_UNAVAILABLE);
        }

        @Test
        @DisplayName("state past its TTL degrades and still records the version it saw")
        void staleStateDegrades() {
            store.put(new CounterpartyRiskState(
                    PAYEE, CounterpartyRiskTier.CONFIRMED, Set.of(NetworkFlag.FAN_IN),
                    List.of(), 40, 7, NOW.minus(Duration.ofMinutes(1441))));

            LayerResult result = evaluator.evaluate(transaction());

            assertThat(result.status()).isEqualTo(LayerStatus.DEGRADED);
            assertThat(codes(result)).containsExactly(ReasonCode.NETWORK_STATE_STALE);
            // Even a stale read is recorded — the audit trail must show what the
            // system saw, including that it was out of date.
            assertThat(result.stateVersion()).isEqualTo(PAYEE + "@v7");
            // A CONFIRMED tier contributed nothing because it was not trusted.
            assertThat(result.contribution()).isZero();
        }

        @Test
        @DisplayName("state just inside the TTL is used")
        void freshStateIsUsed() {
            store.put(new CounterpartyRiskState(
                    PAYEE, CounterpartyRiskTier.HIGH, Set.of(), List.of(), 0, 3,
                    NOW.minus(Duration.ofMinutes(1439))));

            LayerResult result = evaluator.evaluate(transaction());

            assertThat(result.status()).isEqualTo(LayerStatus.EVALUATED);
            assertThat(result.contribution()).isEqualTo(0.70);
        }
    }

    @Nested
    @DisplayName("tiers")
    class Tiers {

        @Test
        @DisplayName("UNKNOWN contributes nothing — absence of data is not risk")
        void unknownTierContributesNothing() {
            store.put(CounterpartyRiskState.unknown(PAYEE, NOW));

            LayerResult result = evaluator.evaluate(transaction());

            assertThat(result.status()).isEqualTo(LayerStatus.EVALUATED);
            assertThat(result.contribution()).isZero();
        }

        @Test
        @DisplayName("LOW contributes nothing — analysed and clean")
        void lowTierContributesNothing() {
            putTier(CounterpartyRiskTier.LOW);

            assertThat(evaluator.evaluate(transaction()).contribution()).isZero();
        }

        @Test
        @DisplayName("adverse tiers contribute their configured weight")
        void adverseTiersContribute() {
            putTier(CounterpartyRiskTier.ELEVATED);
            assertThat(evaluator.evaluate(transaction()).contribution()).isEqualTo(0.40);

            putTier(CounterpartyRiskTier.HIGH);
            assertThat(evaluator.evaluate(transaction()).contribution()).isEqualTo(0.70);

            putTier(CounterpartyRiskTier.CONFIRMED);
            assertThat(evaluator.evaluate(transaction()).contribution()).isEqualTo(1.00);
        }
    }

    @Nested
    @DisplayName("network flags and typologies")
    class Flags {

        @Test
        @DisplayName("a fan-in flag fires and names the distinct payer count")
        void fanInFires() {
            store.put(new CounterpartyRiskState(
                    PAYEE, CounterpartyRiskTier.LOW, Set.of(NetworkFlag.FAN_IN),
                    List.of(), 23, 2, NOW));

            LayerResult result = evaluator.evaluate(transaction());

            assertThat(codes(result)).contains(ReasonCode.COUNTERPARTY_FAN_IN_PATTERN);
            assertThat(result.riskFactors().get(0).explanation()).contains("23 distinct payers");
        }

        @Test
        @DisplayName("a reported typology fires and names it")
        void reportedTypologyFires() {
            store.put(new CounterpartyRiskState(
                    PAYEE, CounterpartyRiskTier.LOW, Set.of(),
                    List.of("MULE_ACCOUNT"), 0, 2, NOW));

            LayerResult result = evaluator.evaluate(transaction());

            assertThat(codes(result)).contains(ReasonCode.COUNTERPARTY_REPORTED_TYPOLOGY);
            assertThat(result.riskFactors().get(0).explanation()).contains("MULE_ACCOUNT");
        }

        @Test
        @DisplayName("a flag that is not FAN_IN does not fire the fan-in rule")
        void otherFlagsDoNotFireFanIn() {
            store.put(new CounterpartyRiskState(
                    PAYEE, CounterpartyRiskTier.LOW, Set.of(NetworkFlag.FAN_OUT),
                    List.of(), 0, 2, NOW));

            assertThat(codes(evaluator.evaluate(transaction())))
                    .doesNotContain(ReasonCode.COUNTERPARTY_FAN_IN_PATTERN);
        }

        @Test
        @DisplayName("contributions accumulate and clamp")
        void contributionsClamp() {
            store.put(new CounterpartyRiskState(
                    PAYEE, CounterpartyRiskTier.HIGH, Set.of(NetworkFlag.FAN_IN),
                    List.of("MULE_ACCOUNT"), 30, 5, NOW));

            // 0.70 + 0.60 + 0.70 = 2.00
            assertThat(evaluator.evaluate(transaction()).contribution()).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("audit")
    class Audit {

        @Test
        @DisplayName("the state version read is recorded on the result")
        void recordsStateVersion() {
            store.put(new CounterpartyRiskState(
                    PAYEE, CounterpartyRiskTier.ELEVATED, Set.of(), List.of(), 0, 12, NOW));

            assertThat(evaluator.evaluate(transaction()).stateVersion())
                    .isEqualTo(PAYEE + "@v12");
        }

        @Test
        @DisplayName("the layer reports itself as in-path")
        void reportsInPath() {
            assertThat(evaluator.layer()).isEqualTo(ControlLayer.COUNTERPARTY_NETWORK);
            assertThat(evaluator.layer().isInPath()).isTrue();
        }
    }

    // --- helpers ------------------------------------------------------------

    private void putTier(CounterpartyRiskTier tier) {
        store.put(new CounterpartyRiskState(PAYEE, tier, Set.of(), List.of(), 0, 1, NOW));
    }

    private static Transaction transaction() {
        return new Transaction("txn-1", "acct-payer", PAYEE, new BigDecimal("100.00"),
                "USD", Channel.MOBILE_APP, "dev-1", Rail.FEDNOW, NOW);
    }

    private static List<ReasonCode> codes(LayerResult result) {
        return result.riskFactors().stream().map(RiskFactor::code).toList();
    }

    /** A store standing in for an unreachable Redis. */
    private static RiskStateStore unavailableStore() {
        return new RiskStateStore() {
            @Override
            public Optional<CounterpartyRiskState> findByCounterpartyId(String counterpartyId) {
                throw new AssertionError(
                        "the evaluator must check availability before attempting a lookup");
            }

            @Override
            public boolean isAvailable() {
                return false;
            }
        };
    }

    private static RuleProperties.NetworkRules networkRules() {
        return new RuleProperties.NetworkRules(
                1440,
                Map.of(
                        CounterpartyRiskTier.UNKNOWN, 0.0,
                        CounterpartyRiskTier.LOW, 0.0,
                        CounterpartyRiskTier.ELEVATED, 0.40,
                        CounterpartyRiskTier.HIGH, 0.70,
                        CounterpartyRiskTier.CONFIRMED, 1.00),
                0.60,
                0.70);
    }

    private static RuleProperties properties() {
        return new RuleProperties(
                Map.of(ControlLayer.COUNTERPARTY_NETWORK, 0.25),
                new RuleProperties.DecisionThresholds(0.35, 0.60),
                new RuleProperties.IdentityRules(30, 0.30, 0.35, 0.35, 0.80, 24),
                new RuleProperties.BehavioralRules(
                        3.0, 0.45, 0.25, 0.20, 5, 0.35, 0.20, 10, new BigDecimal("2500.00")),
                networkRules());
    }
}
