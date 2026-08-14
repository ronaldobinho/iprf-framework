package io.iprf.state;

import java.time.Duration;
import java.time.Instant;

/**
 * Rolling transaction counters per account.
 *
 * <p>The one piece of state written on the payment path. It is permitted
 * because the write is a bounded, in-memory (later Redis) counter increment
 * rather than a query — see {@code docs/framework/architecture.md}, section 6.
 *
 * <p>Reads must never fall back to counting rows in a transaction table. A
 * velocity rule that recounts history on each evaluation is the textbook way to
 * put an unbounded query on the authorization path.
 */
public interface VelocityCounterStore {

    /** Number of payments recorded for this account within the window ending now. */
    int countWithin(String accountId, Duration window, Instant now);

    /** Records a payment. Called once per evaluated transaction. */
    void record(String accountId, Instant at);
}
