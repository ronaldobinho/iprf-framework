package io.iprf.state;

import io.iprf.domain.CounterpartyRiskTier;
import io.iprf.domain.NetworkFlag;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory counterparty risk state.
 *
 * <p>The default implementation, used when Redis is not configured. Registered
 * conditionally in {@code EngineConfig} so the Redis-backed store in the
 * {@code risk-state} module replaces it when present.
 *
 * <p>Keeping a working in-memory implementation is not just a test convenience:
 * it means the framework's core loop can be demonstrated by cloning the
 * repository and running one command, with no infrastructure at all.
 */
public class InMemoryRiskStateStore implements RiskStateStore, RiskStateWriter {

    private final Map<String, CounterpartyRiskState> states = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemoryRiskStateStore(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Optional<CounterpartyRiskState> findByCounterpartyId(String counterpartyId) {
        return counterpartyId == null ? Optional.empty() : Optional.ofNullable(states.get(counterpartyId));
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public long raiseTier(String counterpartyId, CounterpartyRiskTier tier, Collection<NetworkFlag> flags) {
        return states.compute(counterpartyId, (id, existing) -> {
            CounterpartyRiskState current = existing != null
                    ? existing
                    : CounterpartyRiskState.unknown(id, clock.instant());

            // Raise only. A detector running with different inputs must not be
            // able to clear a tier set by a stronger signal.
            CounterpartyRiskTier merged = tier.ordinal() > current.tier().ordinal()
                    ? tier : current.tier();

            Set<NetworkFlag> mergedFlags = EnumSet.noneOf(NetworkFlag.class);
            mergedFlags.addAll(current.flags());
            if (flags != null) {
                mergedFlags.addAll(flags);
            }

            return new CounterpartyRiskState(
                    id, merged, mergedFlags, current.reportedTypologies(),
                    current.distinctPayers(), current.version() + 1, clock.instant());
        }).version();
    }

    @Override
    public long recordTypology(String counterpartyId, String typology) {
        return states.compute(counterpartyId, (id, existing) -> {
            CounterpartyRiskState current = existing != null
                    ? existing
                    : CounterpartyRiskState.unknown(id, clock.instant());

            List<String> typologies = new ArrayList<>(new LinkedHashSet<>(current.reportedTypologies()));
            if (!typologies.contains(typology)) {
                typologies.add(typology);
            }
            return new CounterpartyRiskState(
                    id, current.tier(), current.flags(), typologies,
                    current.distinctPayers(), current.version() + 1, clock.instant());
        }).version();
    }

    @Override
    public long put(CounterpartyRiskState state) {
        states.put(state.counterpartyId(), state);
        return state.version();
    }

    public void clear() {
        states.clear();
    }

    public int size() {
        return states.size();
    }
}
