package io.iprf.engine;

import io.iprf.domain.ControlLayer;
import io.iprf.domain.LayerResult;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Combines per-layer contributions into the composite risk score.
 *
 * <p>A weighted mean over the layers that actually produced a signal. Degraded
 * layers are excluded from the denominator rather than counted as zero: a layer
 * that could not evaluate has said nothing, and treating silence as "no risk"
 * would make a failed dependency look like a safe transaction.
 *
 * <p>That choice has a consequence worth stating — when Layer 3 degrades, the
 * remaining layers carry proportionally more weight, so the score is computed
 * from less evidence. {@link DecisionPolicy} is where that reduced confidence
 * is accounted for.
 */
@Component
public class ScoreComposer {

    private final RuleProperties properties;

    public ScoreComposer(RuleProperties properties) {
        this.properties = properties;
    }

    public double compose(Map<ControlLayer, LayerResult> layerResults) {
        double weightedSum = 0.0;
        double totalWeight = 0.0;

        for (Map.Entry<ControlLayer, LayerResult> entry : layerResults.entrySet()) {
            LayerResult result = entry.getValue();
            if (result.isDegraded()) {
                continue;
            }
            double weight = properties.layerWeights().getOrDefault(entry.getKey(), 0.0);
            weightedSum += result.contribution() * weight;
            totalWeight += weight;
        }

        if (totalWeight == 0.0) {
            // Every layer degraded. Not zero risk — no information. DecisionPolicy
            // handles this case explicitly rather than reading it as an ALLOW.
            return 0.0;
        }
        return clamp(weightedSum / totalWeight);
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
