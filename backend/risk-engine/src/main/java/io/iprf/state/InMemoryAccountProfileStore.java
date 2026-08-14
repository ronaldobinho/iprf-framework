package io.iprf.state;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * In-memory profile store, populated at startup or by asynchronous loaders.
 *
 * <p>Deliberately a plain map. Layer 1's entire latency allocation is 5 ms; a
 * hash lookup keeps the layer's cost structurally irrelevant to the budget.
 *
 * <p>Phase 2 replaces the backing with Redis so that restarting the decision
 * service does not reload state — which is also the recommended remediation for
 * growth-coupled recovery time. See {@code docs/framework/growth-coupling.md}.
 */
@Component
public class InMemoryAccountProfileStore implements AccountProfileStore {

    private final Map<String, AccountProfile> profiles = new ConcurrentHashMap<>();

    @Override
    public Optional<AccountProfile> findByAccountId(String accountId) {
        return accountId == null ? Optional.empty() : Optional.ofNullable(profiles.get(accountId));
    }

    @Override
    public int size() {
        return profiles.size();
    }

    /**
     * Loads profiles. Called by startup loaders and asynchronous updaters —
     * never from the payment path, which is why this method is not on
     * {@link AccountProfileStore}.
     */
    public void put(AccountProfile profile) {
        profiles.put(profile.accountId(), profile);
    }

    public void putAll(Iterable<AccountProfile> batch) {
        batch.forEach(this::put);
    }

    public void clear() {
        profiles.clear();
    }
}
