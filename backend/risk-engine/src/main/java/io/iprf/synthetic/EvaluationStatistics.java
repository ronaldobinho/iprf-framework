package io.iprf.synthetic;

import io.iprf.domain.Decision;
import io.iprf.domain.EvaluationResult;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Accumulates decisions against ground truth and reports the framework's
 * mandated metric set.
 *
 * <p>Every reported figure comes with the others. A detection rate quoted
 * without its accompanying false-positive rate is not a result — it is a
 * selected statistic, and the report is deliberately built so that producing a
 * partial one requires discarding output on purpose. See
 * {@code docs/framework/false-positive-model.md}, section 6.
 */
public class EvaluationStatistics {

    private final Map<Decision, Integer> decisionCounts = new EnumMap<>(Decision.class);
    private final Map<Scenario, Integer> scenarioCounts = new EnumMap<>(Scenario.class);
    private final List<Long> latenciesMicros = new ArrayList<>();

    private int truePositives;
    private int falsePositives;
    private int trueNegatives;
    private int falseNegatives;
    private int hardFalsePositives;   // legitimate AND declined outright
    private int degraded;

    public void record(LabelledTransaction labelled, EvaluationResult result) {
        Decision decision = result.decision();
        decisionCounts.merge(decision, 1, Integer::sum);
        scenarioCounts.merge(labelled.scenario(), 1, Integer::sum);
        latenciesMicros.add(result.totalLatencyMicros());
        if (result.isDegraded()) {
            degraded++;
        }

        // "Flagged" is DECLINE or REVIEW. A REVIEW on a legitimate payment is a
        // false positive: the payment did not complete on the real-time path.
        // Counting only declines would let a control set drive its FP rate to
        // zero by routing everything to review.
        boolean flagged = decision != Decision.ALLOW;

        if (labelled.isFraudulent()) {
            if (flagged) {
                truePositives++;
            } else {
                falseNegatives++;
            }
        } else {
            if (flagged) {
                falsePositives++;
                if (decision == Decision.DECLINE) {
                    hardFalsePositives++;
                }
            } else {
                trueNegatives++;
            }
        }
    }

    public int total() {
        return truePositives + falsePositives + trueNegatives + falseNegatives;
    }

    /** {@code FP / (FP + TN)} — share of legitimate payments that were flagged. */
    public double falsePositiveRate() {
        return ratio(falsePositives, falsePositives + trueNegatives);
    }

    /** Share of legitimate payments declined outright, the subset with no recourse. */
    public double hardFalsePositiveRate() {
        return ratio(hardFalsePositives, falsePositives + trueNegatives);
    }

    /** {@code TP / (TP + FN)} — share of fraudulent payments caught. */
    public double detectionRate() {
        return ratio(truePositives, truePositives + falseNegatives);
    }

    /** {@code TP / (TP + FP)} — share of flagged payments that were actually fraudulent. */
    public double precision() {
        return ratio(truePositives, truePositives + falsePositives);
    }

    /** Operational, not an accuracy metric. Requires no labels. */
    public double rateOf(Decision decision) {
        return ratio(decisionCounts.getOrDefault(decision, 0), total());
    }

    public double degradedRate() {
        return ratio(degraded, total());
    }

    public int countOf(Decision decision) {
        return decisionCounts.getOrDefault(decision, 0);
    }

    public int countOf(Scenario scenario) {
        return scenarioCounts.getOrDefault(scenario, 0);
    }

    public int truePositives() {
        return truePositives;
    }

    public int falsePositives() {
        return falsePositives;
    }

    public int trueNegatives() {
        return trueNegatives;
    }

    public int falseNegatives() {
        return falseNegatives;
    }

    /** Latency at the given percentile, in microseconds. Averages are never reported. */
    public long latencyPercentileMicros(double percentile) {
        if (latenciesMicros.isEmpty()) {
            return 0;
        }
        List<Long> sorted = new ArrayList<>(latenciesMicros);
        sorted.sort(null);
        int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }

    /**
     * The complete report. Detection and false-positive rates, the decision
     * distribution, and latency percentiles always appear together.
     */
    public String report(String datasetDescription) {
        StringBuilder sb = new StringBuilder();
        String line = "-".repeat(72);

        sb.append(line).append('\n');
        sb.append("IPRF scenario run - SYNTHETIC / DEMO DATA\n");
        sb.append(datasetDescription).append('\n');
        sb.append(line).append('\n');

        sb.append("\nDECISION DISTRIBUTION (operational - not accuracy metrics)\n");
        for (Decision decision : Decision.values()) {
            sb.append(pad("  " + decision, 26))
                    .append(pad(String.valueOf(countOf(decision)), 8))
                    .append(pct(rateOf(decision))).append('\n');
        }
        sb.append(pad("  degraded evaluations", 26))
                .append(pad(String.valueOf(degraded), 8))
                .append(pct(degradedRate())).append('\n');

        sb.append("\nGROUND TRUTH (assigned by the generator)\n");
        for (Scenario scenario : Scenario.values()) {
            sb.append(pad("  " + scenario, 26))
                    .append(pad(String.valueOf(countOf(scenario)), 8))
                    .append(scenario.isFraudulent() ? "fraudulent" : "legitimate").append('\n');
        }

        sb.append("\nCONFUSION MATRIX (flagged = DECLINE or REVIEW)\n");
        sb.append(pad("  true positives", 26)).append(truePositives).append('\n');
        sb.append(pad("  false positives", 26)).append(falsePositives)
                .append("   (of which declined outright: ").append(hardFalsePositives).append(")\n");
        sb.append(pad("  true negatives", 26)).append(trueNegatives).append('\n');
        sb.append(pad("  false negatives", 26)).append(falseNegatives).append('\n');

        sb.append("\nRATES\n");
        sb.append(pad("  detection rate", 26)).append(pct(detectionRate())).append('\n');
        sb.append(pad("  false positive rate", 26)).append(pct(falsePositiveRate())).append('\n');
        sb.append(pad("  hard false positive rate", 26)).append(pct(hardFalsePositiveRate())).append('\n');
        sb.append(pad("  precision", 26)).append(pct(precision())).append('\n');

        sb.append("\nIN-PATH LATENCY (budget: 50 ms p99, see docs/framework/latency-model.md)\n");
        sb.append(pad("  p50", 26)).append(millis(latencyPercentileMicros(50))).append('\n');
        sb.append(pad("  p95", 26)).append(millis(latencyPercentileMicros(95))).append('\n');
        sb.append(pad("  p99", 26)).append(millis(latencyPercentileMicros(99))).append('\n');

        sb.append('\n').append(line).append('\n');
        sb.append("""
                These figures measure this rule set against a generated dataset with
                generator-assigned labels. They are reproducible from the seed above and
                are NOT production performance claims. Fraud prevalence in the generator
                is far above real-world rates so a run of this size yields meaningful
                counts.
                """);
        sb.append(line).append('\n');
        return sb.toString();
    }

    private static double ratio(int numerator, int denominator) {
        return denominator == 0 ? 0.0 : (double) numerator / denominator;
    }

    private static String pct(double value) {
        return String.format(Locale.ROOT, "%6.2f%%", value * 100.0);
    }

    private static String millis(long micros) {
        return String.format(Locale.ROOT, "%.3f ms", micros / 1000.0);
    }

    private static String pad(String value, int width) {
        return value.length() >= width ? value : value + " ".repeat(width - value.length());
    }
}
