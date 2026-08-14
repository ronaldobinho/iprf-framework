package io.iprf.engine;

import io.iprf.domain.ControlLayer;
import io.iprf.domain.Decision;
import io.iprf.domain.EvaluationResult;
import io.iprf.domain.LayerResult;
import io.iprf.domain.PathClassification;
import io.iprf.domain.ReasonCode;
import io.iprf.domain.RiskFactor;
import io.iprf.domain.Transaction;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Runs the in-path layers in order, times each one, and composes a decision.
 *
 * <p>Latency measurement is built in from the start rather than added later.
 * A pipeline that cannot report where its time went cannot be held to a budget,
 * and the budget is the thing that makes in-path evaluation safe. See
 * {@code docs/framework/latency-model.md}.
 */
@Component
public class DecisionPipeline {

    private static final Logger log = LoggerFactory.getLogger(DecisionPipeline.class);

    private final List<LayerEvaluator> inPathEvaluators;
    private final ScoreComposer scoreComposer;
    private final DecisionPolicy decisionPolicy;
    private final ExplanationBuilder explanationBuilder;
    private final String frameworkVersion;
    private final Clock clock;

    public DecisionPipeline(
            List<LayerEvaluator> evaluators,
            ScoreComposer scoreComposer,
            DecisionPolicy decisionPolicy,
            ExplanationBuilder explanationBuilder,
            @Value("${iprf.framework-version}") String frameworkVersion,
            Clock clock) {

        // Async layers must never end up on this list. Filtering here rather
        // than trusting registration means a misclassified evaluator is
        // excluded rather than silently placed on the payment path.
        this.inPathEvaluators = evaluators.stream()
                .filter(e -> e.layer().path() == PathClassification.IN_PATH)
                .sorted(Comparator.comparingInt(LayerEvaluator::order))
                .toList();
        this.scoreComposer = scoreComposer;
        this.decisionPolicy = decisionPolicy;
        this.explanationBuilder = explanationBuilder;
        this.frameworkVersion = frameworkVersion;
        this.clock = clock;

        log.info("Decision pipeline initialised with {} in-path evaluators: {}",
                inPathEvaluators.size(),
                inPathEvaluators.stream().map(e -> e.layer().name()).toList());
    }

    /**
     * Evaluates a transaction and returns a fully explainable result.
     *
     * <p>Never throws for a layer failure. A layer that throws is recorded as
     * degraded and the pipeline continues — an exception in one control must not
     * become a failed payment, and it must not become a silent approval either.
     */
    public EvaluationResult evaluate(Transaction transaction, String correlationId) {
        long pipelineStart = System.nanoTime();

        Map<ControlLayer, LayerResult> layerResults = new EnumMap<>(ControlLayer.class);
        List<RiskFactor> allFactors = new ArrayList<>();

        for (LayerEvaluator evaluator : inPathEvaluators) {
            LayerResult result = runLayer(evaluator, transaction);
            layerResults.put(evaluator.layer(), result);
            allFactors.addAll(result.riskFactors());
        }

        double riskScore = scoreComposer.compose(layerResults);
        Decision decision = decisionPolicy.decide(riskScore, layerResults);
        long totalMicros = (System.nanoTime() - pipelineStart) / 1_000;

        EvaluationResult result = new EvaluationResult(
                transaction.transactionId(),
                correlationId,
                decision,
                riskScore,
                allFactors,
                layerResults,
                explanationBuilder.build(decision, riskScore, allFactors, layerResults),
                frameworkVersion,
                clock.instant(),
                totalMicros);

        log.info("evaluated transactionId={} decision={} score={} latencyMicros={} degraded={}",
                transaction.transactionId(), decision,
                String.format(java.util.Locale.ROOT, "%.4f", riskScore), totalMicros, result.isDegraded());

        return result;
    }

    private LayerResult runLayer(LayerEvaluator evaluator, Transaction transaction) {
        long start = System.nanoTime();
        try {
            LayerResult result = evaluator.evaluate(transaction);
            return result.withLatencyMicros((System.nanoTime() - start) / 1_000);
        } catch (RuntimeException e) {
            long micros = (System.nanoTime() - start) / 1_000;
            log.warn("layer={} threw during evaluation of transactionId={}; degrading",
                    evaluator.layer(), transaction.transactionId(), e);
            return LayerResult.degraded(
                    evaluator.layer(), unavailableCodeFor(evaluator.layer()), micros);
        }
    }

    private static ReasonCode unavailableCodeFor(ControlLayer layer) {
        return switch (layer) {
            case IDENTITY_POSTURE -> ReasonCode.IDENTITY_PROFILE_MISSING;
            case BEHAVIORAL_SCORING -> ReasonCode.BASELINE_INSUFFICIENT;
            case COUNTERPARTY_NETWORK -> ReasonCode.NETWORK_STATE_UNAVAILABLE;
            default -> ReasonCode.NO_SIGNAL;
        };
    }
}
