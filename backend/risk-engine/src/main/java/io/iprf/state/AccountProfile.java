package io.iprf.state;

import io.iprf.domain.Channel;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

/**
 * Pre-computed profile of a payer, materialized before the transaction arrives.
 *
 * <p>Every field here is something an implementation would be tempted to query
 * at decision time. That temptation is the single most common way a fraud stack
 * acquires a synchronous database dependency, which is why the profile is a
 * value object loaded from pre-computed state rather than an entity.
 *
 * @param accountId            the payer
 * @param openedAt             account opening timestamp, for account-age rules
 * @param verified             whether identity verification is complete
 * @param knownDeviceIds       devices previously seen for this account
 * @param typicalChannels      channels this account normally transacts on
 * @param baselineAmountMean   mean transaction amount over the baseline window
 * @param baselineAmountStdDev standard deviation over the same window
 * @param knownCounterparties  accounts this payer has paid before
 * @param activeHourStart      first hour of the payer's typical active window, 0-23
 * @param activeHourEnd        last hour of the payer's typical active window, 0-23
 * @param transactionCount     number of transactions the baseline is computed from
 * @param restricted           whether the account carries an active restriction
 * @param computedAt           when this profile was materialized, for staleness checks
 */
public record AccountProfile(
        String accountId,
        Instant openedAt,
        boolean verified,
        Set<String> knownDeviceIds,
        Set<Channel> typicalChannels,
        BigDecimal baselineAmountMean,
        BigDecimal baselineAmountStdDev,
        Set<String> knownCounterparties,
        int activeHourStart,
        int activeHourEnd,
        int transactionCount,
        boolean restricted,
        Instant computedAt) {

    public AccountProfile {
        knownDeviceIds = knownDeviceIds == null ? Set.of() : Set.copyOf(knownDeviceIds);
        typicalChannels = typicalChannels == null ? Set.of() : Set.copyOf(typicalChannels);
        knownCounterparties = knownCounterparties == null ? Set.of() : Set.copyOf(knownCounterparties);
    }

    /**
     * Whether this profile has enough history for deviation rules to be
     * meaningful. Below the threshold, Layer 2 falls back to conservative
     * absolute thresholds rather than comparing against a baseline computed from
     * three transactions.
     */
    public boolean hasSufficientBaseline(int minimumTransactions) {
        return transactionCount >= minimumTransactions
                && baselineAmountMean != null
                && baselineAmountMean.signum() > 0;
    }

    public boolean isKnownDevice(String deviceId) {
        return deviceId != null && knownDeviceIds.contains(deviceId);
    }

    public boolean isKnownCounterparty(String counterpartyId) {
        return counterpartyId != null && knownCounterparties.contains(counterpartyId);
    }
}
