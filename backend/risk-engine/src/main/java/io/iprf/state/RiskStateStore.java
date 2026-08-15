package io.iprf.state;

import java.util.Optional;

/**
 * Read access to pre-computed counterparty risk state.
 *
 * <p><b>Read-only, like every in-path store interface in this framework.</b>
 * Writes happen through {@link RiskStateWriter}, which the in-path modules do
 * not depend on. Separating the two is not ceremony: it makes "Layer 3 cannot
 * populate what it reads" a compile-time property rather than a convention.
 *
 * <p>Implementations must not fall back to a live query when state is missing.
 * A missing entry is an empty {@link Optional} and the caller degrades
 * explicitly.
 */
public interface RiskStateStore {

    /** Pre-computed state for a counterparty, or empty if none exists. */
    Optional<CounterpartyRiskState> findByCounterpartyId(String counterpartyId);

    /**
     * Whether the backing store is reachable.
     *
     * <p>Distinguishes "no state for this counterparty" from "the store is
     * down", which are different findings and produce different reason codes.
     */
    boolean isAvailable();
}
