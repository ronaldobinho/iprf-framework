package io.iprf.state;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * In-memory sliding-window velocity counters.
 *
 * <p>Each account keeps a bounded deque of recent timestamps, pruned on access.
 * Bounded is the operative word: an unbounded structure here would turn a fraud
 * control into a memory leak, and the leak would grow fastest under exactly the
 * burst conditions the rule exists to detect.
 *
 * <p>Phase 2 replaces this with a Redis-backed implementation so counters
 * survive a restart and are shared across instances.
 */
@Component
public class InMemoryVelocityCounterStore implements VelocityCounterStore {

    /** Hard cap on retained timestamps per account. */
    private static final int MAX_RETAINED_PER_ACCOUNT = 256;

    private final Map<String, Deque<Instant>> timestamps = new ConcurrentHashMap<>();

    @Override
    public int countWithin(String accountId, Duration window, Instant now) {
        Deque<Instant> account = timestamps.get(accountId);
        if (account == null) {
            return 0;
        }
        Instant cutoff = now.minus(window);
        synchronized (account) {
            pruneOlderThan(account, cutoff);
            return account.size();
        }
    }

    @Override
    public void record(String accountId, Instant at) {
        Deque<Instant> account = timestamps.computeIfAbsent(accountId, k -> new ArrayDeque<>());
        synchronized (account) {
            account.addLast(at);
            while (account.size() > MAX_RETAINED_PER_ACCOUNT) {
                account.removeFirst();
            }
        }
    }

    public void clear() {
        timestamps.clear();
    }

    private static void pruneOlderThan(Deque<Instant> account, Instant cutoff) {
        while (!account.isEmpty() && account.peekFirst().isBefore(cutoff)) {
            account.removeFirst();
        }
    }
}
