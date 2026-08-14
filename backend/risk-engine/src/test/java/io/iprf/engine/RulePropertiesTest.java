package io.iprf.engine;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.iprf.domain.ControlLayer;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A malformed rule configuration must stop the application at startup rather
 * than produce quietly wrong decisions for as long as nobody notices.
 */
class RulePropertiesTest {

    @Test
    @DisplayName("the shipped configuration shape is valid")
    void validConfigurationIsAccepted() {
        assertThatCode(TestRules::properties).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("inverted thresholds are rejected, because they make REVIEW unreachable")
    void invertedThresholdsRejected() {
        assertThatThrownBy(() -> new RuleProperties.DecisionThresholds(0.80, 0.40))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("REVIEW unreachable");
    }

    @Test
    @DisplayName("equal thresholds are allowed — that is a deliberate no-review policy")
    void equalThresholdsAllowed() {
        assertThatCode(() -> new RuleProperties.DecisionThresholds(0.50, 0.50))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("a threshold outside [0,1] is rejected")
    void outOfRangeThresholdRejected() {
        assertThatThrownBy(() -> new RuleProperties.DecisionThresholds(-0.1, 0.5))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new RuleProperties.DecisionThresholds(0.5, 1.5))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("empty layer weights are rejected — every transaction would score zero")
    void emptyLayerWeightsRejected() {
        assertThatThrownBy(() -> new RuleProperties(
                Map.of(),
                new RuleProperties.DecisionThresholds(0.35, 0.60),
                validIdentity(),
                validBehavioral()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("layer-weights is empty");
    }

    @Test
    @DisplayName("a negative layer weight is rejected")
    void negativeLayerWeightRejected() {
        assertThatThrownBy(() -> new RuleProperties(
                Map.of(ControlLayer.IDENTITY_POSTURE, -0.5),
                new RuleProperties.DecisionThresholds(0.35, 0.60),
                validIdentity(),
                validBehavioral()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("a missing fallback amount is rejected — payers without a baseline "
            + "would face no amount control at all")
    void missingFallbackAmountRejected() {
        assertThatThrownBy(() -> new RuleProperties.BehavioralRules(
                3.0, 0.45, 0.25, 0.20, 5, 0.35, 0.20, 10, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no amount control at all");
    }

    @Test
    @DisplayName("a non-positive velocity window is rejected")
    void nonPositiveVelocityRejected() {
        assertThatThrownBy(() -> new RuleProperties.BehavioralRules(
                3.0, 0.45, 0.25, 0.20, 0, 0.35, 0.20, 10, new BigDecimal("2500.00")))
                .isInstanceOf(IllegalStateException.class);
    }

    private static RuleProperties.IdentityRules validIdentity() {
        return new RuleProperties.IdentityRules(30, 0.30, 0.35, 0.35, 0.80, 24);
    }

    private static RuleProperties.BehavioralRules validBehavioral() {
        return new RuleProperties.BehavioralRules(
                3.0, 0.45, 0.25, 0.20, 5, 0.35, 0.20, 10, new BigDecimal("2500.00"));
    }
}
