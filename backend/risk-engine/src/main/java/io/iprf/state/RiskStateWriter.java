package io.iprf.state;

import io.iprf.domain.CounterpartyRiskTier;
import io.iprf.domain.NetworkFlag;
import java.util.Collection;

/**
 * Write access to pre-computed counterparty risk state.
 *
 * <p>Used only by asynchronous components — Layer 4 enrichment and Layer 5
 * post-settlement analysis. Deliberately a separate interface from
 * {@link RiskStateStore} so that an in-path module cannot acquire write access
 * by accident, and so the ArchUnit guard has something concrete to enforce.
 */
public interface RiskStateWriter {

    /**
     * Raises a counterparty's tier and records the flags that justified it.
     *
     * <p>Raises only. A detection must not be able to lower a tier set by a
     * stronger signal — clearing risk is a separate, deliberate operation, not a
     * side effect of a detector running with different inputs.
     *
     * @return the version written
     */
    long raiseTier(String counterpartyId, CounterpartyRiskTier tier, Collection<NetworkFlag> flags);

    /** Records an externally reported typology against a counterparty. */
    long recordTypology(String counterpartyId, String typology);

    /** Replaces an entry outright. Used by seed loaders, not by detectors. */
    long put(CounterpartyRiskState state);
}
