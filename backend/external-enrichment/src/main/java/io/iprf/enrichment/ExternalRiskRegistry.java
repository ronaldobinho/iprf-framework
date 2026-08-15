package io.iprf.enrichment;

import io.iprf.domain.CounterpartyRiskTier;
import java.util.List;
import java.util.Optional;

/**
 * An external fraud intelligence source.
 *
 * <p>Layer 4's dependency, and the reason Layer 4 is asynchronous. An external
 * call has three properties that are each individually disqualifying for in-path
 * use: unbounded latency (the remote service's p99 is not yours to control),
 * independent availability (its outage becomes your outage), and non-determinism
 * (the same transaction evaluated twice can get different answers).
 *
 * <p>Only ever invoked off the payment path. The ArchUnit guard prevents the
 * in-path modules from even holding an HTTP client, so this cannot be called
 * from one by accident.
 */
public interface ExternalRiskRegistry {

    /** A lookup result. Absent means the registry knows nothing about the account. */
    record Report(String accountId, CounterpartyRiskTier tier, List<String> typologies) {
    }

    /**
     * Queries the registry.
     *
     * @throws ExternalRegistryException when the registry fails or times out.
     *         The caller retries; nothing on the payment path is affected.
     */
    Optional<Report> lookup(String accountId);

    /** Thrown for any registry failure. Never propagates to a payment decision. */
    class ExternalRegistryException extends RuntimeException {
        public ExternalRegistryException(String message) {
            super(message);
        }
    }
}
