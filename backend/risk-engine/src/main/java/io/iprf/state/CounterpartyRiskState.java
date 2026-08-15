package io.iprf.state;

import io.iprf.domain.CounterpartyRiskTier;
import io.iprf.domain.NetworkFlag;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Pre-computed risk state for a receiving account.
 *
 * <p>Written asynchronously by Layers 4 and 5, read in-path by Layer 3. Never
 * computed on the payment path — that is the entire point of it existing.
 *
 * @param counterpartyId    the receiving account
 * @param tier              compressed risk classification
 * @param flags             behavioural patterns detected against this account
 * @param reportedTypologies typology labels from external intelligence or confirmed cases
 * @param distinctPayers    distinct senders observed in the aggregation window
 * @param version           monotonically increasing write version
 * @param computedAt        when this entry was written
 */
public record CounterpartyRiskState(
        String counterpartyId,
        CounterpartyRiskTier tier,
        Set<NetworkFlag> flags,
        List<String> reportedTypologies,
        int distinctPayers,
        long version,
        Instant computedAt) {

    public CounterpartyRiskState {
        Objects.requireNonNull(counterpartyId, "counterpartyId");
        tier = tier == null ? CounterpartyRiskTier.UNKNOWN : tier;
        flags = flags == null ? Set.of() : Set.copyOf(flags);
        reportedTypologies = reportedTypologies == null ? List.of() : List.copyOf(reportedTypologies);
        Objects.requireNonNull(computedAt, "computedAt");
    }

    /**
     * Whether this entry is older than the configured freshness window.
     *
     * <p>Stale state is not silently trusted and not silently discarded — Layer 3
     * degrades with {@code NETWORK_STATE_STALE} so the decision records that it
     * was made without current counterparty information.
     */
    public boolean isStaleAt(Instant now, Duration ttl) {
        return Duration.between(computedAt, now).compareTo(ttl) >= 0;
    }

    /**
     * The version string recorded on the decision and in the audit trail.
     *
     * <p>Reconstructing a decision requires knowing what the system believed at
     * that moment, not what it believes now. Without this, an audit trail
     * describes a decision it cannot reproduce.
     */
    public String versionLabel() {
        return counterpartyId + "@v" + version;
    }

    /** An entry for a counterparty nothing is known about. */
    public static CounterpartyRiskState unknown(String counterpartyId, Instant now) {
        return new CounterpartyRiskState(
                counterpartyId, CounterpartyRiskTier.UNKNOWN, Set.of(), List.of(), 0, 0, now);
    }
}
