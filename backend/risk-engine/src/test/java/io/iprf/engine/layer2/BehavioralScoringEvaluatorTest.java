package io.iprf.engine.layer2;

import static org.assertj.core.api.Assertions.assertThat;

import io.iprf.domain.Channel;
import io.iprf.domain.LayerResult;
import io.iprf.domain.LayerStatus;
import io.iprf.domain.Rail;
import io.iprf.domain.ReasonCode;
import io.iprf.domain.RiskFactor;
import io.iprf.domain.Transaction;
import io.iprf.engine.TestRules;
import io.iprf.state.AccountProfile;
import io.iprf.state.InMemoryAccountProfileStore;
import io.iprf.state.InMemoryVelocityCounterStore;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BehavioralScoringEvaluatorTest {

    private static final Instant NOW = TestRules.NOW;   // 2026-08-14T12:00:00Z

    private InMemoryAccountProfileStore profiles;
    private InMemoryVelocityCounterStore velocity;
    private BehavioralScoringEvaluator evaluator;

    @BeforeEach
    void setUp() {
        profiles = new InMemoryAccountProfileStore();
        velocity = new InMemoryVelocityCounterStore();
        evaluator = new BehavioralScoringEvaluator(
                profiles, velocity, TestRules.properties(), TestRules.CLOCK);
        profiles.put(TestRules.establishedProfile());
    }

    @Nested
    @DisplayName("amount deviation")
    class AmountDeviation {

        // Profile: mean 100.00, stdDev 20.00, threshold = 3 sigma = 160.00

        @Test
        @DisplayName("just under three sigma does not fire")
        void justUnderThreshold() {
            LayerResult result = evaluate(new BigDecimal("159.99"));

            assertThat(codes(result)).doesNotContain(ReasonCode.AMOUNT_DEVIATION_HIGH);
        }

        @Test
        @DisplayName("exactly three sigma fires")
        void exactlyAtThreshold() {
            LayerResult result = evaluate(new BigDecimal("160.00"));

            assertThat(codes(result)).contains(ReasonCode.AMOUNT_DEVIATION_HIGH);
            assertThat(result.contribution()).isEqualTo(0.45);
        }

        @Test
        @DisplayName("the explanation states the deviation in sigmas, not just the amount")
        void explanationIsSpecific() {
            LayerResult result = evaluate(new BigDecimal("200.00"));

            RiskFactor factor = result.riskFactors().stream()
                    .filter(f -> f.code() == ReasonCode.AMOUNT_DEVIATION_HIGH)
                    .findFirst().orElseThrow();
            assertThat(factor.explanation())
                    .contains("5.0 standard deviations")
                    .contains("100.00");
        }
    }

    @Nested
    @DisplayName("insufficient baseline")
    class InsufficientBaseline {

        @Test
        @DisplayName("a thin history falls back to an absolute threshold and says so")
        void fallsBackToAbsoluteThreshold() {
            profiles.put(thinHistoryProfile());

            LayerResult result = evaluate("acct-thin", new BigDecimal("3000.00"));

            assertThat(codes(result))
                    .contains(ReasonCode.BASELINE_INSUFFICIENT)
                    .contains(ReasonCode.AMOUNT_DEVIATION_HIGH);
        }

        @Test
        @DisplayName("a thin history below the absolute threshold still records the fallback")
        void recordsFallbackEvenWhenNotFiring() {
            profiles.put(thinHistoryProfile());

            LayerResult result = evaluate("acct-thin", new BigDecimal("50.00"));

            // The transaction is not flagged on amount, but the audit trail must
            // show that no baseline was available to compare it against.
            assertThat(codes(result)).contains(ReasonCode.BASELINE_INSUFFICIENT);
            assertThat(codes(result)).doesNotContain(ReasonCode.AMOUNT_DEVIATION_HIGH);
        }

        @Test
        @DisplayName("an unknown payer degrades the whole layer")
        void unknownPayerDegrades() {
            LayerResult result = evaluate("acct-nobody", new BigDecimal("100.00"));

            assertThat(result.status()).isEqualTo(LayerStatus.DEGRADED);
            assertThat(codes(result)).containsExactly(ReasonCode.BASELINE_INSUFFICIENT);
        }
    }

    @Nested
    @DisplayName("individual signals")
    class Signals {

        @Test
        @DisplayName("a payment matching the baseline in every dimension scores zero")
        void ordinaryPaymentScoresZero() {
            LayerResult result = evaluate(new BigDecimal("100.00"));

            assertThat(result.status()).isEqualTo(LayerStatus.EVALUATED);
            assertThat(result.contribution()).isZero();
        }

        @Test
        @DisplayName("a new counterparty alone does not reach the review threshold")
        void newCounterpartyAloneIsWeak() {
            LayerResult result = evaluate(transaction(
                    "acct-established", "acct-brand-new", new BigDecimal("100.00"),
                    Channel.MOBILE_APP, "dev-known", atHour(14)));

            assertThat(codes(result)).containsExactly(ReasonCode.COUNTERPARTY_NEW);
            // Paying a new landlord must not flag on its own — this is the
            // single most important false-positive guard in the layer.
            assertThat(result.contribution()).isEqualTo(0.25);
        }

        @Test
        @DisplayName("a payment outside the payer's active hours fires")
        void unusualHourFires() {
            LayerResult result = evaluate(transaction(
                    "acct-established", "acct-known-payee", new BigDecimal("100.00"),
                    Channel.MOBILE_APP, "dev-known", atHour(3)));

            assertThat(codes(result)).contains(ReasonCode.TIMING_UNUSUAL_HOUR);
        }

        @Test
        @DisplayName("a payment inside the active window does not fire on timing")
        void withinActiveHoursDoesNotFire() {
            LayerResult result = evaluate(transaction(
                    "acct-established", "acct-known-payee", new BigDecimal("100.00"),
                    Channel.MOBILE_APP, "dev-known", atHour(8)));

            assertThat(codes(result)).doesNotContain(ReasonCode.TIMING_UNUSUAL_HOUR);
        }

        @Test
        @DisplayName("an atypical channel fires")
        void channelSwitchFires() {
            LayerResult result = evaluate(transaction(
                    "acct-established", "acct-known-payee", new BigDecimal("100.00"),
                    Channel.API, "dev-known", atHour(14)));

            assertThat(codes(result)).contains(ReasonCode.CHANNEL_UNUSUAL);
        }

        @Test
        @DisplayName("velocity fires once the window count reaches the configured maximum")
        void velocityFiresAtThreshold() {
            // Configured maximum is 5 per hour.
            for (int i = 0; i < 5; i++) {
                velocity.record("acct-established", NOW.minus(Duration.ofMinutes(10 + i)));
            }

            LayerResult result = evaluate(new BigDecimal("100.00"));

            assertThat(codes(result)).contains(ReasonCode.VELOCITY_WINDOW_EXCEEDED);
        }

        @Test
        @DisplayName("payments outside the rolling window do not count toward velocity")
        void velocityWindowSlides() {
            for (int i = 0; i < 5; i++) {
                velocity.record("acct-established", NOW.minus(Duration.ofMinutes(61 + i)));
            }

            LayerResult result = evaluate(new BigDecimal("100.00"));

            assertThat(codes(result)).doesNotContain(ReasonCode.VELOCITY_WINDOW_EXCEEDED);
        }

        @Test
        @DisplayName("a transaction does not inflate its own velocity reading")
        void doesNotCountItself() {
            for (int i = 0; i < 4; i++) {
                velocity.record("acct-established", NOW.minus(Duration.ofMinutes(10 + i)));
            }

            // Four prior payments plus this one. If the current transaction were
            // recorded before counting, this would read as five and fire.
            LayerResult result = evaluate(new BigDecimal("100.00"));

            assertThat(codes(result)).doesNotContain(ReasonCode.VELOCITY_WINDOW_EXCEEDED);
        }
    }

    @Nested
    @DisplayName("composition")
    class Composition {

        @Test
        @DisplayName("weak signals combine into a meaningful contribution")
        void signalsCombine() {
            LayerResult result = evaluate(transaction(
                    "acct-established", "acct-brand-new", new BigDecimal("300.00"),
                    Channel.API, "dev-known", atHour(3)));

            // amount 0.45 + new counterparty 0.25 + hour 0.20 + channel 0.20
            assertThat(codes(result)).contains(
                    ReasonCode.AMOUNT_DEVIATION_HIGH,
                    ReasonCode.COUNTERPARTY_NEW,
                    ReasonCode.TIMING_UNUSUAL_HOUR,
                    ReasonCode.CHANNEL_UNUSUAL);
            assertThat(result.contribution()).isEqualTo(1.0);   // clamped from 1.10
        }

        @Test
        @DisplayName("a self-transfer does not fire the counterparty rule")
        void selfTransferSkipsCounterparty() {
            LayerResult result = evaluate(transaction(
                    "acct-established", "acct-established", new BigDecimal("100.00"),
                    Channel.MOBILE_APP, "dev-known", atHour(14)));

            assertThat(codes(result)).doesNotContain(ReasonCode.COUNTERPARTY_NEW);
        }
    }

    // --- helpers ------------------------------------------------------------

    private LayerResult evaluate(BigDecimal amount) {
        return evaluate("acct-established", amount);
    }

    private LayerResult evaluate(String payer, BigDecimal amount) {
        return evaluator.evaluate(transaction(
                payer, "acct-known-payee", amount, Channel.MOBILE_APP, "dev-known", atHour(14)));
    }

    private LayerResult evaluate(Transaction transaction) {
        return evaluator.evaluate(transaction);
    }

    private static Transaction transaction(
            String payer, String payee, BigDecimal amount,
            Channel channel, String deviceId, Instant at) {
        return new Transaction("txn-1", payer, payee, amount, "USD",
                channel, deviceId, Rail.FEDNOW, at);
    }

    private static Instant atHour(int hour) {
        return Instant.parse("2026-08-14T00:00:00Z").plus(Duration.ofHours(hour));
    }

    private static List<ReasonCode> codes(LayerResult result) {
        return result.riskFactors().stream().map(RiskFactor::code).toList();
    }

    private static AccountProfile thinHistoryProfile() {
        return new AccountProfile(
                "acct-thin",
                NOW.minus(Duration.ofDays(400)),
                true,
                Set.of("dev-known"),
                Set.of(Channel.MOBILE_APP),
                new BigDecimal("100.00"),
                new BigDecimal("20.00"),
                Set.of("acct-known-payee"),
                8, 20,
                3,                                  // below the 10-transaction minimum
                false,
                NOW.minus(Duration.ofMinutes(5)));
    }
}
