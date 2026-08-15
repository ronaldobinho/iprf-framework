package io.iprf.events;

import java.time.Instant;

/**
 * An internal event.
 *
 * <p>Every event carries the same four things, and each exists for a reason:
 *
 * <ul>
 *   <li>{@link #eventId()} is the idempotency key. Message delivery is
 *       at-least-once, so a consumer that is not keyed by this will eventually
 *       double-count a settlement or double-raise a risk tier.
 *   <li>{@link #correlationId()} is propagated from the originating request, so
 *       a decision can be traced across the async boundary — where the work
 *       happens minutes or hours later, and correlating by timestamp is guessing.
 *   <li>{@link #occurredAt()} is when the fact happened, not when it was
 *       published. Retries and backlogs make those diverge.
 *   <li>{@link #schemaVersion()} exists because consumers deploy at different
 *       times. Fields are added, never repurposed.
 * </ul>
 */
public interface IprfEvent {

    /** Stable unique identifier. The idempotency key for every consumer. */
    String eventId();

    /** Correlation identifier of the request that ultimately caused this event. */
    String correlationId();

    /** When the fact occurred — not when the message was published. */
    Instant occurredAt();

    /** Stable event type name, used for routing and in the idempotency store. */
    String eventType();

    /** Schema version. Additive changes only. */
    default int schemaVersion() {
        return 1;
    }
}
