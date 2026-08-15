package io.iprf.events;

/**
 * Records which events each handler has already processed.
 *
 * <p>Scoped per handler, not globally: two handlers legitimately both process
 * the same event, and a global key would let whichever ran first suppress the
 * other.
 */
public interface IdempotencyStore {

    /**
     * Claims an event for a handler.
     *
     * @return {@code true} if this is the first time the handler has seen the
     *         event and it should be processed; {@code false} if it is a
     *         duplicate delivery and must be a no-op.
     */
    boolean claim(String handlerName, String eventId);

    /** Number of claims recorded. Exposed for reporting and tests. */
    int size();
}
