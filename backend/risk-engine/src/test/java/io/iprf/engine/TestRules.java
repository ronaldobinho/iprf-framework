package io.iprf.engine;

import io.iprf.domain.Channel;
import io.iprf.domain.ControlLayer;
import io.iprf.domain.CounterpartyRiskTier;
import io.iprf.state.AccountProfile;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;

/**
 * Fixtures for rule tests.
 *
 * <p>The rule values mirror {@code application-rules.yml}. They are restated
 * here rather than loaded so a test asserting a threshold boundary keeps
 * asserting that boundary when someone tunes the shipped configuration — a
 * boundary test that silently follows the config is not testing anything.
 */
public final class TestRules {

    /** A fixed instant, so account-age and active-hour assertions are stable. */
    public static final Instant NOW = Instant.parse("2026-08-14T12:00:00Z");

    public static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private TestRules() {
    }

    public static RuleProperties properties() {
        return new RuleProperties(
                Map.of(
                        ControlLayer.IDENTITY_POSTURE, 0.25,
                        ControlLayer.BEHAVIORAL_SCORING, 0.50,
                        ControlLayer.COUNTERPARTY_NETWORK, 0.25),
                new RuleProperties.DecisionThresholds(0.35, 0.60),
                new RuleProperties.IdentityRules(30, 0.30, 0.35, 0.35, 0.80, 24),
                new RuleProperties.BehavioralRules(
                        3.0, 0.45, 0.25, 0.20, 5, 0.35, 0.20, 10, new BigDecimal("2500.00")),
                networkRules());
    }

    public static RuleProperties.NetworkRules networkRules() {
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

    /**
     * An established payer with a clean posture: old account, verified, known
     * device, known counterparty, active 08:00-20:00, mean 100 stddev 20.
     */
    public static AccountProfile establishedProfile() {
        return new AccountProfile(
                "acct-established",
                NOW.minus(Duration.ofDays(500)),
                true,
                Set.of("dev-known"),
                Set.of(Channel.MOBILE_APP),
                new BigDecimal("100.00"),
                new BigDecimal("20.00"),
                Set.of("acct-known-payee"),
                8,
                20,
                250,
                false,
                NOW.minus(Duration.ofMinutes(5)));
    }
}
