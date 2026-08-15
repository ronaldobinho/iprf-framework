package io.iprf.events;

/**
 * Publishes an internal event.
 *
 * <p><b>Implementations must return without waiting for any consumer.</b> This is
 * the framework's core principle expressed at the transport layer: an event
 * published from the payment path must not be able to make the payment path
 * slower, and a consumer that hangs must not hang the response.
 *
 * <p>A publisher that blocks would silently convert Layers 4 and 5 into in-path
 * work, defeating the classification without any code appearing to violate it.
 */
public interface EventPublisher {

    /** Publishes without blocking. Never throws for a consumer-side failure. */
    void publish(IprfEvent event);
}
