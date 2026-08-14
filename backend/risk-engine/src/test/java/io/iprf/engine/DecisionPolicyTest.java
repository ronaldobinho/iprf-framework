package io.iprf.engine;

import static org.assertj.core.api.Assertions.assertThat;

import io.iprf.domain.ControlLayer;
import io.iprf.domain.Decision;
import io.iprf.domain.LayerResult;
import io.iprf.domain.LayerStatus;
import io.iprf.domain.ReasonCode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DecisionPolicyTest {

    private final DecisionPolicy policy = new DecisionPolicy(TestRules.properties());

    @Nested
    @DisplayName("thresholds")
    class Thresholds {

        @ParameterizedTest(name = "score {0} -> {1}")
        @CsvSource({
                "0.00, ALLOW",
                "0.34, ALLOW",
                "0.3499, ALLOW",
                "0.35, REVIEW",     // boundary is inclusive at review
                "0.50, REVIEW",
                "0.5999, REVIEW",
                "0.60, DECLINE",    // boundary is inclusive at decline
                "1.00, DECLINE",
        })
        @DisplayName("boundaries are inclusive at the upper band")
        void boundaries(double score, Decision expected) {
            assertThat(policy.decide(score, healthyLayers())).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("degradation must never become a silent approval")
    class Degradation {

        @Test
        @DisplayName("when every layer degrades the decision is REVIEW, not ALLOW")
        void allDegradedBecomesReview() {
            Map<ControlLayer, LayerResult> layers = new EnumMap<>(ControlLayer.class);
            layers.put(ControlLayer.IDENTITY_POSTURE, degraded(ControlLayer.IDENTITY_POSTURE));
            layers.put(ControlLayer.BEHAVIORAL_SCORING, degraded(ControlLayer.BEHAVIORAL_SCORING));

            // Score is 0.0 because nothing evaluated — on thresholds alone this
            // would approve, which is exactly the failure this rule prevents.
            assertThat(policy.decide(0.0, layers)).isEqualTo(Decision.REVIEW);
        }

        @Test
        @DisplayName("a degraded Layer 1 escalates ALLOW to REVIEW")
        void degradedIdentityEscalates() {
            Map<ControlLayer, LayerResult> layers = new EnumMap<>(ControlLayer.class);
            layers.put(ControlLayer.IDENTITY_POSTURE, degraded(ControlLayer.IDENTITY_POSTURE));
            layers.put(ControlLayer.BEHAVIORAL_SCORING, healthy(ControlLayer.BEHAVIORAL_SCORING));

            assertThat(policy.decide(0.10, layers)).isEqualTo(Decision.REVIEW);
        }

        @Test
        @DisplayName("a degraded Layer 2 escalates ALLOW to REVIEW")
        void degradedBehaviouralEscalates() {
            Map<ControlLayer, LayerResult> layers = new EnumMap<>(ControlLayer.class);
            layers.put(ControlLayer.IDENTITY_POSTURE, healthy(ControlLayer.IDENTITY_POSTURE));
            layers.put(ControlLayer.BEHAVIORAL_SCORING, degraded(ControlLayer.BEHAVIORAL_SCORING));

            assertThat(policy.decide(0.10, layers)).isEqualTo(Decision.REVIEW);
        }

        @Test
        @DisplayName("a degraded Layer 3 does NOT escalate — it is designed to be droppable")
        void degradedNetworkDoesNotEscalate() {
            Map<ControlLayer, LayerResult> layers = new EnumMap<>(ControlLayer.class);
            layers.put(ControlLayer.IDENTITY_POSTURE, healthy(ControlLayer.IDENTITY_POSTURE));
            layers.put(ControlLayer.BEHAVIORAL_SCORING, healthy(ControlLayer.BEHAVIORAL_SCORING));
            layers.put(ControlLayer.COUNTERPARTY_NETWORK, degraded(ControlLayer.COUNTERPARTY_NETWORK));

            // Layers 1-2 evaluated successfully and are sufficient to decide.
            // Escalating here would convert every Redis blip into a review queue.
            assertThat(policy.decide(0.10, layers)).isEqualTo(Decision.ALLOW);
        }

        @Test
        @DisplayName("degradation never softens a DECLINE")
        void degradationNeverSoftensDecline() {
            Map<ControlLayer, LayerResult> layers = new EnumMap<>(ControlLayer.class);
            layers.put(ControlLayer.IDENTITY_POSTURE, degraded(ControlLayer.IDENTITY_POSTURE));
            layers.put(ControlLayer.BEHAVIORAL_SCORING, healthy(ControlLayer.BEHAVIORAL_SCORING));

            assertThat(policy.decide(0.90, layers)).isEqualTo(Decision.DECLINE);
        }
    }

    // --- helpers ------------------------------------------------------------

    private static Map<ControlLayer, LayerResult> healthyLayers() {
        Map<ControlLayer, LayerResult> layers = new EnumMap<>(ControlLayer.class);
        layers.put(ControlLayer.IDENTITY_POSTURE, healthy(ControlLayer.IDENTITY_POSTURE));
        layers.put(ControlLayer.BEHAVIORAL_SCORING, healthy(ControlLayer.BEHAVIORAL_SCORING));
        return layers;
    }

    private static LayerResult healthy(ControlLayer layer) {
        return new LayerResult(layer, LayerStatus.EVALUATED, 0.0, List.of(), 100, null);
    }

    private static LayerResult degraded(ControlLayer layer) {
        return LayerResult.degraded(layer, ReasonCode.NO_SIGNAL, 100);
    }
}
