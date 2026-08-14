package io.iprf.synthetic;

import io.iprf.domain.Transaction;

/**
 * A generated transaction with its ground-truth label.
 *
 * <p>In production, obtaining this label is the hardest problem in fraud
 * measurement: labels arrive late, arrive incomplete, and are biased by the
 * control itself — you never learn the outcome of a payment you declined. This
 * repository sidesteps that problem by not pretending to solve it, and assigns
 * the label at generation time instead.
 *
 * <p>Consequently every rate computed from these labels is exactly as meaningful
 * as the generator's realism, and no more. See
 * {@code docs/framework/false-positive-model.md}.
 */
public record LabelledTransaction(Transaction transaction, Scenario scenario) {

    public boolean isFraudulent() {
        return scenario.isFraudulent();
    }
}
