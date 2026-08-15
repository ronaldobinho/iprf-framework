package io.iprf.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * A transaction that has settled.
 *
 * <p>Layer 5's input. Settlement is the point at which the money has actually
 * moved and the pattern becomes real — analysing authorization attempts instead
 * would count declined payments as evidence of a receiver's activity.
 *
 * @param transaction the payment
 * @param settledAt   when settlement completed
 */
public record SettledTransaction(Transaction transaction, Instant settledAt) {

    public SettledTransaction {
        Objects.requireNonNull(transaction, "transaction");
        Objects.requireNonNull(settledAt, "settledAt");
    }

    public String payerAccountId() {
        return transaction.payerAccountId();
    }

    public String payeeAccountId() {
        return transaction.payeeAccountId();
    }
}
