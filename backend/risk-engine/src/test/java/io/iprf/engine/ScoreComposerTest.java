package io.iprf.engine;

import static org.assertj.core.api.Assertions.assertThat;

import io.iprf.domain.ControlLayer;
import io.iprf.domain.LayerResult;
import io.iprf.domain.LayerStatus;
import io.iprf.domain.ReasonCode;
import io.iprf.domain.RiskFactor;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.assertj.core.data.Offset;

class ScoreComposerTest {

    private static final Offset<Double> TOLERANCE = Offset.offset(1e-9);

    private final ScoreComposer composer = new ScoreComposer(TestRules.properties());

    // Configured weights: identity 0.25, behavioural 0.50, network 0.25.

    @Test
    @DisplayName("contributions are a weighted mean over the layers that evaluated")
    void weightedMean() {
        Map<ControlLayer, LayerResult> layers = new EnumMap<>(ControlLayer.class);
        layers.put(ControlLayer.IDENTITY_POSTURE, evaluated(ControlLayer.IDENTITY_POSTURE, 0.40));
        layers.put(ControlLayer.BEHAVIORAL_SCORING, evaluated(ControlLayer.BEHAVIORAL_SCORING, 0.60));
        layers.put(ControlLayer.COUNTERPARTY_NETWORK, evaluated(ControlLayer.COUNTERPARTY_NETWORK, 0.20));

        // (0.40*0.25 + 0.60*0.50 + 0.20*0.25) / 1.00 = 0.45
        assertThat(composer.compose(layers)).isCloseTo(0.45, TOLERANCE);
    }

    @Nested
    @DisplayName("layers that said nothing are excluded from the denominator")
    class Exclusions {

        @Test
        @DisplayName("a NO_DATA layer does not dilute the score")
        void noDataIsExcluded() {
            Map<ControlLayer, LayerResult> withNoData = new EnumMap<>(ControlLayer.class);
            withNoData.put(ControlLayer.IDENTITY_POSTURE, evaluated(ControlLayer.IDENTITY_POSTURE, 0.40));
            withNoData.put(ControlLayer.BEHAVIORAL_SCORING, evaluated(ControlLayer.BEHAVIORAL_SCORING, 0.60));
            withNoData.put(ControlLayer.COUNTERPARTY_NETWORK, noData(ControlLayer.COUNTERPARTY_NETWORK));

            Map<ControlLayer, LayerResult> withoutLayerThree = new EnumMap<>(ControlLayer.class);
            withoutLayerThree.put(ControlLayer.IDENTITY_POSTURE, evaluated(ControlLayer.IDENTITY_POSTURE, 0.40));
            withoutLayerThree.put(ControlLayer.BEHAVIORAL_SCORING, evaluated(ControlLayer.BEHAVIORAL_SCORING, 0.60));

            // An unknown counterparty must score exactly the same as no Layer 3
            // at all. If NO_DATA sat in the denominator contributing zero, the
            // score would drop — and silence would be acting as evidence of
            // safety, which the framework says it must never be.
            assertThat(composer.compose(withNoData))
                    .isCloseTo(composer.compose(withoutLayerThree), TOLERANCE);

            // (0.40*0.25 + 0.60*0.50) / 0.75 = 0.5333...
            assertThat(composer.compose(withNoData)).isCloseTo(0.4 / 3 + 0.4, TOLERANCE);
        }

        @Test
        @DisplayName("a degraded layer does not dilute the score either")
        void degradedIsExcluded() {
            Map<ControlLayer, LayerResult> layers = new EnumMap<>(ControlLayer.class);
            layers.put(ControlLayer.IDENTITY_POSTURE, evaluated(ControlLayer.IDENTITY_POSTURE, 0.80));
            layers.put(ControlLayer.BEHAVIORAL_SCORING,
                    LayerResult.degraded(ControlLayer.BEHAVIORAL_SCORING, ReasonCode.NO_SIGNAL, 0));

            // Only Layer 1 evaluated, so the score is Layer 1's contribution.
            assertThat(composer.compose(layers)).isCloseTo(0.80, TOLERANCE);
        }

        @Test
        @DisplayName("when nothing evaluated the score is zero — DecisionPolicy handles the rest")
        void nothingEvaluated() {
            Map<ControlLayer, LayerResult> layers = new EnumMap<>(ControlLayer.class);
            layers.put(ControlLayer.IDENTITY_POSTURE,
                    LayerResult.degraded(ControlLayer.IDENTITY_POSTURE, ReasonCode.NO_SIGNAL, 0));

            // Zero here means "no information", not "no risk". It is
            // DecisionPolicy that stops this becoming an ALLOW.
            assertThat(composer.compose(layers)).isZero();
        }
    }

    @Test
    @DisplayName("the composite is clamped to [0, 1]")
    void clamped() {
        Map<ControlLayer, LayerResult> layers = new EnumMap<>(ControlLayer.class);
        layers.put(ControlLayer.IDENTITY_POSTURE, evaluated(ControlLayer.IDENTITY_POSTURE, 1.0));
        layers.put(ControlLayer.BEHAVIORAL_SCORING, evaluated(ControlLayer.BEHAVIORAL_SCORING, 1.0));

        assertThat(composer.compose(layers)).isEqualTo(1.0);
    }

    @Test
    @DisplayName("an empty result set scores zero rather than dividing by zero")
    void emptyIsSafe() {
        assertThat(composer.compose(new EnumMap<>(ControlLayer.class))).isZero();
    }

    private static LayerResult evaluated(ControlLayer layer, double contribution) {
        return new LayerResult(layer, LayerStatus.EVALUATED, contribution, List.of(), 10, null);
    }

    private static LayerResult noData(ControlLayer layer) {
        return new LayerResult(layer, LayerStatus.NO_DATA, 0.0,
                List.of(RiskFactor.degraded(ReasonCode.NETWORK_STATE_ABSENT, layer)), 10, null);
    }
}
