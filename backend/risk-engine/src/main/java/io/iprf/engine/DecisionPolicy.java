package io.iprf.engine;

import io.iprf.domain.ControlLayer;
import io.iprf.domain.Decision;
import io.iprf.domain.LayerResult;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Turns a composite score into a decision, and enforces the framework's rule
 * that <b>a degraded evaluation must never silently become an {@code ALLOW}</b>.
 *
 * <p>Thresholds alone are not sufficient. A pipeline that scored 0.0 because
 * every layer failed would, on thresholds alone, approve the payment — and it
 * would do so precisely under the conditions an attacker would like to create.
 * See {@code docs/framework/latency-model.md}, section 4.
 */
@Component
public class DecisionPolicy {

    private final RuleProperties properties;

    public DecisionPolicy(RuleProperties properties) {
        this.properties = properties;
    }

    public Decision decide(double riskScore, Map<ControlLayer, LayerResult> layerResults) {
        Decision scored = fromThresholds(riskScore);

        // Nothing evaluated successfully: the score carries no information at all.
        if (!layerResults.isEmpty() && layerResults.values().stream().allMatch(LayerResult::isDegraded)) {
            return Decision.REVIEW;
        }

        // Layers 1-2 are the primary signal for a payer-initiated payment. If
        // either degraded, the remaining evidence cannot support a confident
        // approval, so ALLOW is escalated to REVIEW. Layer 3 degrading does not
        // escalate: it is designed to contribute nothing when unavailable, and
        // Layers 1-2 are sufficient to decide.
        if (scored == Decision.ALLOW && primarySignalDegraded(layerResults)) {
            return Decision.REVIEW;
        }

        return scored;
    }

    private Decision fromThresholds(double riskScore) {
        RuleProperties.DecisionThresholds t = properties.decision();
        if (riskScore >= t.declineThreshold()) {
            return Decision.DECLINE;
        }
        if (riskScore >= t.reviewThreshold()) {
            return Decision.REVIEW;
        }
        return Decision.ALLOW;
    }

    private static boolean primarySignalDegraded(Map<ControlLayer, LayerResult> layerResults) {
        return isDegraded(layerResults, ControlLayer.IDENTITY_POSTURE)
                || isDegraded(layerResults, ControlLayer.BEHAVIORAL_SCORING);
    }

    private static boolean isDegraded(Map<ControlLayer, LayerResult> results, ControlLayer layer) {
        LayerResult result = results.get(layer);
        return result != null && result.isDegraded();
    }
}
