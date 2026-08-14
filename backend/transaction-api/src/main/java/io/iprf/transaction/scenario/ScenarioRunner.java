package io.iprf.transaction.scenario;

import io.iprf.domain.EvaluationResult;
import io.iprf.engine.DecisionPipeline;
import io.iprf.state.AccountProfile;
import io.iprf.state.InMemoryAccountProfileStore;
import io.iprf.synthetic.EvaluationStatistics;
import io.iprf.synthetic.LabelledTransaction;
import io.iprf.synthetic.SyntheticDatasetGenerator;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Runs a synthetic scenario end-to-end and prints the decision distribution and
 * accuracy metrics, then exits.
 *
 * <p>Activated only when {@code iprf.scenario.enabled=true}, so it can never run
 * in a serving deployment. Invoke with:
 *
 * <pre>{@code ./gradlew runScenario}</pre>
 *
 * <p>Deterministic given the seed — anyone running the same command gets the
 * same numbers, which is the difference between a reproducible measurement and
 * a claim.
 */
@Component
@ConditionalOnProperty(name = "iprf.scenario.enabled", havingValue = "true")
public class ScenarioRunner implements ApplicationRunner {

    private final DecisionPipeline pipeline;
    private final InMemoryAccountProfileStore profileStore;
    private final ScenarioProperties properties;
    private final ApplicationContext context;
    private final Clock clock;

    public ScenarioRunner(
            DecisionPipeline pipeline,
            InMemoryAccountProfileStore profileStore,
            ScenarioProperties properties,
            ApplicationContext context,
            Clock clock) {
        this.pipeline = pipeline;
        this.profileStore = profileStore;
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

        EvaluationStatistics statistics = new EvaluationStatistics();
        for (LabelledTransaction labelled : dataset) {
            EvaluationResult result = pipeline.evaluate(
                    labelled.transaction(), UUID.randomUUID().toString());
            statistics.record(labelled, result);
        }

        System.out.println(statistics.report(
                "profiles=%d  transactions=%d  seed=%d  frameworkVersion=%s".formatted(
                        properties.profiles(),
                        properties.transactions(),
                        properties.seed(),
                        properties.frameworkVersion())));

        // The scenario run is a batch job, not a service. Exit rather than
        // leaving an HTTP listener bound.
        System.exit(org.springframework.boot.SpringApplication.exit(context, () -> 0));
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
