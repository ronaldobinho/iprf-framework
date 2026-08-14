package io.iprf.engine.layer1;

import static org.assertj.core.api.Assertions.assertThat;

import io.iprf.domain.Channel;
import io.iprf.domain.LayerResult;
import io.iprf.domain.LayerStatus;
import io.iprf.domain.Rail;
import io.iprf.domain.ReasonCode;
import io.iprf.domain.RiskFactor;
import io.iprf.domain.Transaction;
import io.iprf.engine.RuleProperties;
import io.iprf.engine.TestRules;
import io.iprf.state.AccountProfile;
import io.iprf.state.InMemoryAccountProfileStore;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class IdentityPostureEvaluatorTest {

    private static final Instant NOW = Instant.parse("2026-08-14T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private InMemoryAccountProfileStore store;
    private IdentityPostureEvaluator evaluator;

    @BeforeEach
    void setUp() {
        store = new InMemoryAccountProfileStore();
        evaluator = new IdentityPostureEvaluator(store, TestRules.properties(), CLOCK);
    }

    @Nested
    @DisplayName("degradation")
    class Degradation {

        @Test
        @DisplayName("an unknown payer degrades rather than scoring zero")
        void unknownPayerDegrades() {
            LayerResult result = evaluator.evaluate(transaction("acct-nobody", "dev-known"));

            assertThat(result.status()).isEqualTo(LayerStatus.DEGRADED);
            assertThat(result.isDegraded()).isTrue();
            assertThat(codes(result)).containsExactly(ReasonCode.IDENTITY_PROFILE_MISSING);
            // Zero contribution, but flagged as degraded — DecisionPolicy is what
            // stops that zero from reading as "safe".
            assertThat(result.contribution()).isZero();
        }

        @Test
        @DisplayName("a profile older than the freshness window degrades")
        void staleProfileDegrades() {
            store.put(profileBuilder().computedAt(NOW.minus(Duration.ofHours(25))).build());

            LayerResult result = evaluator.evaluate(transaction("acct-test", "dev-known"));

            assertThat(result.status()).isEqualTo(LayerStatus.DEGRADED);
            assertThat(codes(result)).containsExactly(ReasonCode.IDENTITY_PROFILE_STALE);
        }

        @Test
        @DisplayName("a profile just inside the freshness window evaluates normally")
        void freshProfileEvaluates() {
            store.put(profileBuilder().computedAt(NOW.minus(Duration.ofHours(23))).build());

            LayerResult result = evaluator.evaluate(transaction("acct-test", "dev-known"));

            assertThat(result.status()).isEqualTo(LayerStatus.EVALUATED);
        }
    }

    @Nested
    @DisplayName("threshold boundaries")
    class Boundaries {

        @Test
        @DisplayName("an account one day under the threshold fires the age rule")
        void justUnderAgeThreshold() {
            store.put(profileBuilder().openedAt(NOW.minus(Duration.ofDays(29))).build());

            LayerResult result = evaluator.evaluate(transaction("acct-test", "dev-known"));

            assertThat(codes(result)).contains(ReasonCode.ACCOUNT_AGE_LOW);
            assertThat(result.contribution()).isEqualTo(0.30);
        }

        @Test
        @DisplayName("an account exactly at the threshold does not fire")
        void exactlyAtAgeThreshold() {
            store.put(profileBuilder().openedAt(NOW.minus(Duration.ofDays(30))).build());

            LayerResult result = evaluator.evaluate(transaction("acct-test", "dev-known"));

            assertThat(codes(result)).doesNotContain(ReasonCode.ACCOUNT_AGE_LOW);
            assertThat(result.contribution()).isZero();
        }
    }

    @Nested
    @DisplayName("rules")
    class Rules {

        @Test
        @DisplayName("a clean established payer produces no contribution")
        void cleanProfileScoresZero() {
            store.put(profileBuilder().build());

            LayerResult result = evaluator.evaluate(transaction("acct-test", "dev-known"));

            assertThat(result.status()).isEqualTo(LayerStatus.EVALUATED);
            assertThat(result.contribution()).isZero();
            assertThat(result.riskFactors()).isEmpty();
        }

        @Test
        @DisplayName("an unrecognised device fires")
        void unknownDeviceFires() {
            store.put(profileBuilder().build());

            LayerResult result = evaluator.evaluate(transaction("acct-test", "dev-stranger"));

            assertThat(codes(result)).containsExactly(ReasonCode.DEVICE_UNKNOWN);
            assertThat(result.contribution()).isEqualTo(0.35);
        }

        @Test
        @DisplayName("a missing device identifier fires, and says so specifically")
        void absentDeviceFires() {
            store.put(profileBuilder().build());

            LayerResult result = evaluator.evaluate(transaction("acct-test", null));

            assertThat(codes(result)).containsExactly(ReasonCode.DEVICE_UNKNOWN);
            assertThat(result.riskFactors().get(0).explanation())
                    .contains("No device identifier was supplied");
        }

        @Test
        @DisplayName("contributions accumulate and clamp at 1.0")
        void contributionsAccumulateAndClamp() {
            store.put(profileBuilder()
                    .openedAt(NOW.minus(Duration.ofDays(2)))   // 0.30
                    .verified(false)                            // 0.35
                    .restricted(true)                           // 0.80
                    .build());

            // plus unknown device 0.35 — sums to 1.80, must clamp
            LayerResult result = evaluator.evaluate(transaction("acct-test", "dev-stranger"));

            assertThat(result.riskFactors()).hasSize(4);
            assertThat(result.contribution()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("every factor carries the rule id and version for the audit trail")
        void factorsCarryRuleVersions() {
            store.put(profileBuilder().build());

            LayerResult result = evaluator.evaluate(transaction("acct-test", "dev-stranger"));

            assertThat(result.riskFactors())
                    .allSatisfy(f -> {
                        assertThat(f.ruleId()).isNotBlank();
                        assertThat(f.ruleVersion()).isNotBlank();
                    });
        }
    }

    // --- helpers ------------------------------------------------------------

    private static List<ReasonCode> codes(LayerResult result) {
        return result.riskFactors().stream().map(RiskFactor::code).toList();
    }

    private static Transaction transaction(String payer, String deviceId) {
        return new Transaction("txn-1", payer, "acct-known-payee",
                new BigDecimal("100.00"), "USD", Channel.MOBILE_APP, deviceId,
                Rail.FEDNOW, NOW);
    }


    private static ProfileBuilder profileBuilder() {
        return new ProfileBuilder();
    }

    /** Small builder so each test varies exactly one attribute. */
    private static final class ProfileBuilder {
        private Instant openedAt = NOW.minus(Duration.ofDays(500));
        private boolean verified = true;
        private boolean restricted = false;
        private Instant computedAt = NOW.minus(Duration.ofMinutes(5));

        ProfileBuilder openedAt(Instant value) {
            this.openedAt = value;
            return this;
        }

        ProfileBuilder verified(boolean value) {
            this.verified = value;
            return this;
        }

        ProfileBuilder restricted(boolean value) {
            this.restricted = value;
            return this;
        }

        ProfileBuilder computedAt(Instant value) {
            this.computedAt = value;
            return this;
        }

        AccountProfile build() {
            return new AccountProfile(
                    "acct-test", openedAt, verified,
                    Set.of("dev-known"), Set.of(Channel.MOBILE_APP),
                    new BigDecimal("100.00"), new BigDecimal("20.00"),
                    Set.of("acct-known-payee"), 8, 20, 250, restricted, computedAt);
        }
    }
}
