package io.iprf.transaction.scenario;

import io.iprf.domain.Decision;
import io.iprf.domain.EvaluationResult;
import io.iprf.domain.SettledTransaction;
import io.iprf.engine.DecisionPipeline;
import io.iprf.postsettlement.PatternDetection;
import io.iprf.postsettlement.PostSettlementAnalyzer;
import io.iprf.state.AccountProfile;
import io.iprf.state.InMemoryAccountProfileStore;
import io.iprf.state.InMemoryVelocityCounterStore;
import io.iprf.synthetic.EvaluationStatistics;
import io.iprf.synthetic.LabelledTransaction;
import io.iprf.synthetic.SyntheticDatasetGenerator;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Runs a synthetic scenario end-to-end in two passes, demonstrating the
 * framework's feedback loop, then exits.
 *
 * <p>Pass one evaluates the dataset with an empty risk state, which is what a
 * system with no post-settlement analysis looks like. Allowed payments then
 * settle, Layer 5 analyses the settled set and writes what it finds into
 * pre-computed state, and pass two re-evaluates the same dataset.
 *
 * <p><b>Nothing in this tells the engine which accounts are mules.</b> The
 * generator's labels are used only to score the results. The tiers Layer 3 reads
 * in pass two were derived by Layer 5 from observed settlement patterns — if the
 * detection rate improves, it improved on evidence the system found for itself.
 *
 * <p>Re-evaluating the same dataset is artificial: in production these would be
 * different, later transactions. It is done this way so the effect of the
 * feedback loop is isolated from every other difference between two samples.
 *
 * <p>Invoke with {@code ./gradlew runScenario}. Deterministic given the seed.
 */
@Component
@ConditionalOnProperty(name = "iprf.scenario.enabled", havingValue = "true")
public class ScenarioRunner implements ApplicationRunner {

    private final DecisionPipeline pipeline;
    private final PostSettlementAnalyzer analyzer;
    private final InMemoryAccountProfileStore profileStore;
    private final InMemoryVelocityCounterStore velocityStore;
    private final ScenarioProperties properties;
    private final ApplicationContext context;
    private final Clock clock;

    public ScenarioRunner(
            DecisionPipeline pipeline,
            PostSettlementAnalyzer analyzer,
            InMemoryAccountProfileStore profileStore,
            InMemoryVelocityCounterStore velocityStore,
            ScenarioProperties properties,
            ApplicationContext context,
            Clock clock) {
        this.pipeline = pipeline;
        this.analyzer = analyzer;
        this.profileStore = profileStore;
        this.velocityStore = velocityStore;
        this.properties = properties;
        this.context = context;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        Instant now = clock.instant();
        SyntheticDatasetGenerator generator = new SyntheticDatasetGenerator(properties.seed());

        List<AccountProfile> profiles = generator.generateProfiles(properties.profiles(), now);
        profileStore.putAll(profiles);

        List<LabelledTransaction> dataset =
                generator.generateTransactions(profiles, properties.transactions(), now);

        String description = "profiles=%d  transactions=%d  seed=%d  frameworkVersion=%s".formatted(
                properties.profiles(), properties.transactions(),
                properties.seed(), properties.frameworkVersion());

        // --- Pass 1: no counterparty intelligence ---------------------------
        Pass baseline = evaluate(dataset);
        System.out.println(baseline.statistics.report(
                "PASS 1 of 2 - before post-settlement analysis (Layer 3 has no state)\n" + description));

        // --- Settlement and Layer 5 analysis --------------------------------
        // Only allowed payments settle. Analysing authorization attempts would
        // count declined payments as evidence of a receiver's activity.
        List<SettledTransaction> settled = baseline.settled;
        List<PatternDetection> detections = analyzer.analyse(settled);
        System.out.println(detectionReport(settled.size(), detections));

        // --- Pass 2: with the feedback loop closed --------------------------
        // Velocity counters are reset so the delta isolates the Layer 3 effect
        // rather than measuring pass 1's own traffic.
        velocityStore.clear();
        Pass informed = evaluate(dataset);
        System.out.println(informed.statistics.report(
                "PASS 2 of 2 - after post-settlement analysis fed Layer 3\n" + description));

        System.out.println(deltaReport(baseline.statistics, informed.statistics));

        System.exit(SpringApplication.exit(context, () -> 0));
    }

    private Pass evaluate(List<LabelledTransaction> dataset) {
        EvaluationStatistics statistics = new EvaluationStatistics();
        List<SettledTransaction> settled = new ArrayList<>();

        for (LabelledTransaction labelled : dataset) {
            EvaluationResult result =
                    pipeline.evaluate(labelled.transaction(), UUID.randomUUID().toString());
            statistics.record(labelled, result);
            if (result.decision() == Decision.ALLOW) {
                settled.add(new SettledTransaction(
                        labelled.transaction(), labelled.transaction().initiatedAt()));
            }
        }
        return new Pass(statistics, settled);
    }

    private String detectionReport(int settledCount, List<PatternDetection> detections) {
        StringBuilder sb = new StringBuilder();
        String line = "-".repeat(72);
        sb.append('\n').append(line).append('\n');
        sb.append("LAYER 5 - post-settlement analysis (ASYNC, off the payment path)\n");
        sb.append(line).append('\n');
        sb.append("  settled transactions analysed  ").append(settledCount).append('\n');
        sb.append("  detectors run                  ")
                .append(String.join(", ", analyzer.detectorNames())).append('\n');
        sb.append("  detections                     ").append(detections.size()).append('\n');

        if (!detections.isEmpty()) {
            sb.append("\n  Accounts whose risk tier was raised (first 10):\n");
            detections.stream().limit(10).forEach(d -> sb
                    .append("    ").append(d.accountId())
                    .append("  ").append(d.flag())
                    .append(" -> ").append(d.tier())
                    .append("  (").append(d.evidence()).append(")\n"));
        }
        sb.append('\n').append("  These tiers were derived from observed settlement patterns.\n")
                .append("  The generator's fraud labels were NOT used to produce them.\n");
        sb.append(line).append('\n');
        return sb.toString();
    }

    private static String deltaReport(EvaluationStatistics before, EvaluationStatistics after) {
        StringBuilder sb = new StringBuilder();
        String line = "=".repeat(72);
        sb.append('\n').append(line).append('\n');
        sb.append("FEEDBACK LOOP EFFECT - pass 1 vs pass 2\n");
        sb.append(line).append('\n');
        sb.append(row("detection rate", before.detectionRate(), after.detectionRate()));
        sb.append(row("false positive rate", before.falsePositiveRate(), after.falsePositiveRate()));
        sb.append(row("hard false positive rate",
                before.hardFalsePositiveRate(), after.hardFalsePositiveRate()));
        sb.append(row("precision", before.precision(), after.precision()));
        sb.append('\n');
        sb.append("  A detection rate that rises while the false-positive rate holds is the\n");
        sb.append("  loop working. A rise in both means the detectors are simply flagging more\n");
        sb.append("  counterparties, which is a different and less useful result.\n");
        sb.append(line).append('\n');
        return sb.toString();
    }

    private static String row(String label, double before, double after) {
        double delta = after - before;
        return String.format(java.util.Locale.ROOT,
                "  %-26s %7.2f%%  ->  %7.2f%%   (%+.2f pp)%n",
                label, before * 100, after * 100, delta * 100);
    }

    private record Pass(EvaluationStatistics statistics, List<SettledTransaction> settled) {
    }

    /**
     * @param enabled          activates the runner
     * @param profiles         number of payer profiles to generate
     * @param transactions     number of transactions to evaluate
     * @param seed             makes the run reproducible
     * @param frameworkVersion stamped on the report for provenance
     */
    @ConfigurationProperties(prefix = "iprf.scenario")
    public record ScenarioProperties(
            boolean enabled,
            int profiles,
            int transactions,
            long seed,
            String frameworkVersion) {

        public ScenarioProperties {
            if (profiles <= 0) {
                profiles = 200;
            }
            if (transactions <= 0) {
                transactions = 1_000;
            }
            if (frameworkVersion == null) {
                frameworkVersion = "unknown";
            }
        }
    }
}
