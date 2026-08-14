package io.iprf.engine;

import io.iprf.domain.ControlLayer;
import io.iprf.domain.LayerResult;
import io.iprf.domain.Transaction;

/**
 * One control layer's evaluation step.
 *
 * <p>Implementations of in-path layers must honour the contract in
 * {@code docs/framework/fraud-control-layers.md}: deterministic, bounded,
 * reading pre-computed state only, degrading explicitly. The build enforces the
 * pre-computed-reads condition — an ArchUnit test fails if these modules import
 * a JPA or repository type.
 *
 * <p>Implementations do not measure their own latency and do not throw for
 * missing input. Missing input is a {@link LayerResult#degraded} result, which
 * is a normal outcome rather than an error.
 */
public interface LayerEvaluator {

    /** The layer this evaluator implements. */
    ControlLayer layer();

    /**
     * Evaluates the transaction.
     *
     * <p>Must not block, must not query, must not call out. The returned
     * result's latency field is ignored — the pipeline stamps the measured
     * value.
     */
    LayerResult evaluate(Transaction transaction);

    /** Evaluation order within the pipeline; lower runs first. Defaults to layer number. */
    default int order() {
        return layer().number();
    }
}
