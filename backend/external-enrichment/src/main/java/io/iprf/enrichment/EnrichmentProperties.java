package io.iprf.enrichment;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Layer 4 configuration, including the simulated registry's behaviour.
 *
 * <p>Latency and failure rate are configurable so that the framework's async
 * guarantee can be <em>tested</em> rather than asserted: a test sets the stub to
 * hang and verifies that decision latency is unaffected. A stub that was always
 * fast would make that test impossible to write, and the guarantee would rest on
 * reading the code and believing it.
 *
 * @param enabled            whether Layer 4 runs at all
 * @param simulatedLatencyMs artificial delay on each registry lookup
 * @param simulatedFailureRate proportion of lookups that fail, in {@code [0, 1]}
 * @param knownAccountRate   proportion of queried accounts the registry knows about
 * @param seed               makes the simulated registry deterministic
 */
@ConfigurationProperties(prefix = "iprf.enrichment")
public record EnrichmentProperties(
        boolean enabled,
        long simulatedLatencyMs,
        double simulatedFailureRate,
        double knownAccountRate,
        long seed) {

    public EnrichmentProperties {
        if (simulatedLatencyMs < 0) {
            throw new IllegalStateException("enrichment.simulated-latency-ms must not be negative");
        }
        if (simulatedFailureRate < 0.0 || simulatedFailureRate > 1.0) {
            throw new IllegalStateException(
                    "enrichment.simulated-failure-rate must be within [0, 1]");
        }
        if (knownAccountRate < 0.0 || knownAccountRate > 1.0) {
            throw new IllegalStateException(
                    "enrichment.known-account-rate must be within [0, 1]");
        }
    }
}
