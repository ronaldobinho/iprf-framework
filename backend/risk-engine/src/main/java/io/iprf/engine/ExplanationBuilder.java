package io.iprf.engine;

import io.iprf.domain.ControlLayer;
import io.iprf.domain.Decision;
import io.iprf.domain.LayerResult;
import io.iprf.domain.RiskFactor;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Builds the human-readable explanation carried on every decision.
 *
 * <p>Written for someone who has to defend the decision — to the customer whose
 * payment was declined, to a risk committee, or to a supervisor. It names the
 * factors that drove the score in order of contribution, and it states when the
 * decision was made on incomplete information.
 */
@Component
public class ExplanationBuilder {

    private static final int MAX_FACTORS_NAMED = 3;

    /**
     * Explanations are formatted with {@link Locale#ROOT}, never the platform
     * default. On a machine with a comma-decimal locale the default would emit
     * "0,42" into an API response — breaking numeric parsing for consumers and
     * diverging from the TypeScript simulator, whose CI parity test requires
     * byte-identical output.
     */
    private static final Locale FORMAT_LOCALE = Locale.ROOT;

    public String build(
            Decision decision,
            double riskScore,
            List<RiskFactor> factors,
            Map<ControlLayer, LayerResult> layerResults) {

        StringBuilder sb = new StringBuilder();
        sb.append(verdictFor(decision))
                .append(" with a composite risk score of ")
                .append(String.format(FORMAT_LOCALE, "%.2f", riskScore))
                .append(".");

        List<RiskFactor> contributing = factors.stream()
                .filter(f -> f.contribution() > 0.0)
                .sorted(Comparator.comparingDouble(RiskFactor::contribution).reversed())
                .limit(MAX_FACTORS_NAMED)
                .toList();

        if (contributing.isEmpty()) {
            sb.append(" No rule produced a risk contribution.");
        } else {
            sb.append(" Principal factors: ")
                    .append(contributing.stream()
                            .map(f -> f.explanation() + " (" + f.code() + ", contribution "
                                    + String.format(FORMAT_LOCALE, "%.2f", f.contribution()) + ")")
                            .collect(Collectors.joining("; ")))
                    .append(".");
        }

        List<String> degraded = layerResults.values().stream()
                .filter(LayerResult::isDegraded)
                .map(r -> r.layer().displayName())
                .toList();

        if (!degraded.isEmpty()) {
            sb.append(" This decision was made on incomplete input: ")
                    .append(String.join(", ", degraded))
                    .append(degraded.size() == 1 ? " did not evaluate." : " did not evaluate.");
        }

        return sb.toString();
    }

    private static String verdictFor(Decision decision) {
        return switch (decision) {
            case ALLOW -> "Approved for the real-time path";
            case REVIEW -> "Held for review";
            case DECLINE -> "Declined";
        };
    }
}
