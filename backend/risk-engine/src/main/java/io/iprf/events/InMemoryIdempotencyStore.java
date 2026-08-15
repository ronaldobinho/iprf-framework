package io.iprf.events;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * In-memory idempotency claims.
 *
 * <p>Bounded. An unbounded set here would be a memory leak that grows fastest
 * under exactly the event volume it is meant to survive. Eviction is
 * insertion-ordered: the oldest claims go first, on the reasoning that duplicate
 * deliveries arrive close together in time.
 *
 * <p>That bound is a real limitation, and it is why Phase 2 also provides a
 * durable implementation: a redelivery arriving after eviction would be
 * processed twice.
 */
public class InMemoryIdempotencyStore implements IdempotencyStore {

    private static final int DEFAULT_CAPACITY = 100_000;

    private final int capacity;
    private final Set<String> claims;

    public InMemoryIdempotencyStore() {
        this(DEFAULT_CAPACITY);
    }

    public InMemoryIdempotencyStore(int capacity) {
        this.capacity = capacity;
        this.claims = Collections.synchronizedSet(new LinkedHashSet<>());
    }

    @Override
    public boolean claim(String handlerName, String eventId) {
        String key = handlerName + "::" + eventId;
        synchronized (claims) {
            if (!claims.add(key)) {
                return false;
            }
            while (claims.size() > capacity) {
                var iterator = claims.iterator();
                iterator.next();
                iterator.remove();
            }
            return true;
        }
    }

    @Override
    public int size() {
        return claims.size();
    }

    public void clear() {
        claims.clear();
    }
}
