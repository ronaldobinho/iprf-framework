package io.iprf.events;

/**
 * Consumes internal events.
 *
 * <p>Handlers are invoked off the payment path and may be slow, may fail, and
 * may be retried. What they must be is <b>idempotent</b>: delivery is
 * at-least-once, so a handler that is not keyed by {@link IprfEvent#eventId()}
 * will eventually double-count a settlement or double-raise a risk tier.
 *
 * <p>The dispatcher enforces this rather than trusting each handler to remember:
 * it checks the idempotency store before invoking {@link #handle}, so a
 * duplicate delivery is a logged no-op.
 */
public interface EventHandler {

    /** Stable name. Used as the idempotency scope, so two handlers can each process the same event. */
    String name();

    /** Whether this handler is interested in the given event type. */
    boolean handles(String eventType);

    /** Processes the event. Called at most once per event id, per handler. */
    void handle(IprfEvent event);
}
