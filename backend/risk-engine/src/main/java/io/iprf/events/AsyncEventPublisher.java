package io.iprf.events;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default publisher: hands events to a bounded executor and returns.
 *
 * <p>Dispatching on a separate executor is not an optimisation, it is the
 * structural guarantee. {@link #publish} performs a queue offer and returns —
 * whatever a handler does afterwards, however slowly, cannot reach back into the
 * caller's latency. That property is asserted by a test in which a handler
 * blocks indefinitely.
 *
 * <p>Idempotency is enforced here rather than in each handler, so a handler
 * author cannot forget it. Delivery is at-least-once by design; a handler seeing
 * a duplicate is normal, and it must be a no-op rather than a second settlement
 * count or a second tier raise.
 *
 * <p>Phase 6 replaces the executor with a RabbitMQ binding for deployments that
 * need durability across restarts. The interface does not change, which is the
 * point of there being one.
 */
public class AsyncEventPublisher implements EventPublisher, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(AsyncEventPublisher.class);

    /**
     * Resolved lazily on first publish.
     *
     * <p>A handler may legitimately publish events of its own — Layer 4 emits
     * {@code ExternalRiskUpdated} after enriching — which makes the publisher and
     * its handlers mutually dependent at construction time. Deferring resolution
     * breaks that cycle at the publisher, which is the honest place for it: the
     * publisher genuinely does not need the handler list until something is
     * published.
     */
    private final Supplier<List<EventHandler>> handlerSupplier;
    private volatile List<EventHandler> resolvedHandlers;

    private final IdempotencyStore idempotencyStore;
    private final ExecutorService executor;

    private final AtomicLong published = new AtomicLong();
    private final AtomicLong duplicatesSuppressed = new AtomicLong();
    private final AtomicLong handlerFailures = new AtomicLong();
    private final AtomicLong rejected = new AtomicLong();

    public AsyncEventPublisher(
            Supplier<List<EventHandler>> handlerSupplier, IdempotencyStore idempotencyStore) {
        this(handlerSupplier, idempotencyStore, defaultExecutor());
    }

    AsyncEventPublisher(
            Supplier<List<EventHandler>> handlerSupplier,
            IdempotencyStore idempotencyStore,
            ExecutorService executor) {
        this.handlerSupplier = handlerSupplier;
        this.idempotencyStore = idempotencyStore;
        this.executor = executor;
    }

    private List<EventHandler> handlers() {
        List<EventHandler> local = resolvedHandlers;
        if (local == null) {
            synchronized (this) {
                local = resolvedHandlers;
                if (local == null) {
                    local = List.copyOf(handlerSupplier.get());
                    resolvedHandlers = local;
                    log.info("Async event publisher resolved {} handler(s): {}",
                            local.size(), local.stream().map(EventHandler::name).toList());
                }
            }
        }
        return local;
    }

    @Override
    public void publish(IprfEvent event) {
        published.incrementAndGet();
        for (EventHandler handler : handlers()) {
            if (!handler.handles(event.eventType())) {
                continue;
            }
            try {
                executor.execute(() -> dispatch(handler, event));
            } catch (RejectedExecutionException e) {
                // The queue is saturated or the executor is shutting down.
                // Dropping an async event degrades future decision quality; it
                // must never propagate to the caller and fail a payment.
                rejected.incrementAndGet();
                log.warn("event dispatch rejected handler={} eventId={} type={}",
                        handler.name(), event.eventId(), event.eventType());
            }
        }
    }

    private void dispatch(EventHandler handler, IprfEvent event) {
        if (!idempotencyStore.claim(handler.name(), event.eventId())) {
            duplicatesSuppressed.incrementAndGet();
            log.debug("duplicate delivery suppressed handler={} eventId={}",
                    handler.name(), event.eventId());
            return;
        }
        try {
            handler.handle(event);
        } catch (RuntimeException e) {
            handlerFailures.incrementAndGet();
            log.warn("handler={} failed for eventId={} type={}",
                    handler.name(), event.eventId(), event.eventType(), e);
        }
    }

    /**
     * Waits for in-flight dispatches to finish.
     *
     * <p>For tests and for the scenario runner, which needs the async work to
     * have landed before it measures the effect. Production code never calls
     * this — waiting for async work on the payment path is the thing this class
     * exists to prevent.
     */
    public boolean awaitQuiescence(long timeout, TimeUnit unit) throws InterruptedException {
        executor.shutdown();
        return executor.awaitTermination(timeout, unit);
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }

    public long publishedCount() {
        return published.get();
    }

    public long duplicatesSuppressedCount() {
        return duplicatesSuppressed.get();
    }

    public long handlerFailureCount() {
        return handlerFailures.get();
    }

    public long rejectedCount() {
        return rejected.get();
    }

    private static ExecutorService defaultExecutor() {
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable, "iprf-events");
            // Daemon: async event processing must never hold up JVM shutdown.
            thread.setDaemon(true);
            return thread;
        };
        return Executors.newFixedThreadPool(
                Math.max(2, Runtime.getRuntime().availableProcessors() / 2), factory);
    }
}
