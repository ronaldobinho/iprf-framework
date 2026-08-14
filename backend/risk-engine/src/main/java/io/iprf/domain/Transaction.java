package io.iprf.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * A payment being evaluated.
 *
 * <p>Immutable by construction. The decision pipeline must not be able to mutate
 * its input: reproducing a historical decision requires the inputs to be exactly
 * what they were, and a mutable transaction object makes that guarantee
 * unverifiable.
 *
 * @param transactionId  institution-assigned identifier, unique per payment
 * @param payerAccountId account the funds leave
 * @param payeeAccountId account the funds arrive at
 * @param amount         transaction amount; {@link BigDecimal} because this is money
 * @param currency       ISO 4217 code
 * @param channel        how the payment was initiated
 * @param deviceId       device identifier where the channel provides one, else {@code null}
 * @param rail           the instant-payment rail
 * @param initiatedAt    when the payer's institution received the payment order
 */
public record Transaction(
        String transactionId,
        String payerAccountId,
        String payeeAccountId,
        BigDecimal amount,
        String currency,
        Channel channel,
        String deviceId,
        Rail rail,
        Instant initiatedAt) {

    public Transaction {
        Objects.requireNonNull(transactionId, "transactionId");
        Objects.requireNonNull(payerAccountId, "payerAccountId");
        Objects.requireNonNull(payeeAccountId, "payeeAccountId");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        Objects.requireNonNull(initiatedAt, "initiatedAt");
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive, was " + amount);
        }
        channel = channel == null ? Channel.UNKNOWN : channel;
        rail = rail == null ? Rail.OTHER : rail;
    }

    /** True when the payer is paying themselves — excluded from counterparty rules. */
    public boolean isSelfTransfer() {
        return payerAccountId.equals(payeeAccountId);
    }
}
