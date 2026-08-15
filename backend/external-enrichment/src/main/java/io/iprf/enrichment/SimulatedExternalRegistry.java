package io.iprf.enrichment;

import io.iprf.domain.CounterpartyRiskTier;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * An in-repository stand-in for a shared fraud registry.
 *
 * <p><b>Simulated on purpose.</b> The framework specifies the integration point;
 * it does not provide a network. Depending on a real third-party service would
 * make this repository impossible to run without credentials, impossible to test
 * deterministically, and would put someone else's availability in the way of a
 * demonstration.
 *
 * <p>Its verdicts are derived from a hash of the account identifier, so they are
 * stable across runs without any stored data — the same account always gets the
 * same answer. That is enough to demonstrate the loop and honest about being a
 * simulation: <b>it contains no real intelligence about any account.</b>
 */
@Component
public class SimulatedExternalRegistry implements ExternalRiskRegistry {

    private final EnrichmentProperties properties;

    public SimulatedExternalRegistry(EnrichmentProperties properties) {
        this.properties = properties;
    }

    @Override
    public Optional<Report> lookup(String accountId) {
        sleepSimulatedLatency();

        if (properties.simulatedFailureRate() > 0.0
                && ThreadLocalRandom.current().nextDouble() < properties.simulatedFailureRate()) {
            throw new ExternalRegistryException(
                    "simulated registry failure for accountId=" + accountId);
        }

        // Deterministic per account: mixing the configured seed with the account
        // hash means a given account gets the same verdict every run, which is
        // what makes a scenario reproducible.
        long mixed = Integer.toUnsignedLong(accountId.hashCode()) ^ properties.seed();
        double roll = (mixed % 10_000) / 10_000.0;

        if (roll >= properties.knownAccountRate()) {
            return Optional.empty();
        }

        // Within the known set, most accounts are clean. A registry that
        // reported adverse findings for a large share of accounts would raise
        // tiers indiscriminately and drive up the false-positive rate of every
        // later decision.
        double severity = ((mixed / 10_000) % 100) / 100.0;
        if (severity < 0.80) {
            return Optional.of(new Report(accountId, CounterpartyRiskTier.LOW, List.of()));
        }
        if (severity < 0.95) {
            return Optional.of(new Report(
                    accountId, CounterpartyRiskTier.ELEVATED, List.of("EXTERNALLY_REPORTED")));
        }
        return Optional.of(new Report(
                accountId, CounterpartyRiskTier.HIGH, List.of("EXTERNALLY_REPORTED_MULE")));
    }

    private void sleepSimulatedLatency() {
        if (properties.simulatedLatencyMs() <= 0) {
            return;
        }
        try {
            Thread.sleep(properties.simulatedLatencyMs());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExternalRegistryException("interrupted while querying registry");
        }
    }
}
