package io.iprf.events;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AsyncEventPublisherTest {

    private AsyncEventPublisher publisher;

    @AfterEach
    void tearDown() {
        if (publisher != null) {
            publisher.close();
        }
    }

    @Nested
    @DisplayName("idempotency — delivery is at-least-once by design")
    class Idempotency {

        @Test
        @DisplayName("a duplicate delivery is a no-op, not a second side effect")
        void duplicateDeliveryIsNoOp() throws Exception {
            CountingHandler handler = new CountingHandler("counter");
            publisher = publisherWith(handler);

            IprfEvent event = event("evt-1");
            publisher.publish(event);
            publisher.publish(event);   // same event id — redelivery
            publisher.publish(event);

            publisher.awaitQuiescence(5, TimeUnit.SECONDS);

            // Without idempotency this would be three settlement counts, or
            // three tier raises against the same account.
            assertThat(handler.invocations.get()).isEqualTo(1);
            assertThat(publisher.duplicatesSuppressedCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("distinct events are all processed")
        void distinctEventsAllProcessed() throws Exception {
            CountingHandler handler = new CountingHandler("counter");
            publisher = publisherWith(handler);

            publisher.publish(event("evt-1"));
            publisher.publish(event("evt-2"));
            publisher.publish(event("evt-3"));

            publisher.awaitQuiescence(5, TimeUnit.SECONDS);

            assertThat(handler.invocations.get()).isEqualTo(3);
            assertThat(publisher.duplicatesSuppressedCount()).isZero();
        }

        @Test
        @DisplayName("idempotency is scoped per handler, so two handlers both see the event")
        void idempotencyIsPerHandler() throws Exception {
            CountingHandler first = new CountingHandler("first");
            CountingHandler second = new CountingHandler("second");
            publisher = publisherWith(first, second);

            publisher.publish(event("evt-1"));
            publisher.publish(event("evt-1"));

            publisher.awaitQuiescence(5, TimeUnit.SECONDS);

            // A global key would let whichever handler ran first suppress the other.
            assertThat(first.invocations.get()).isEqualTo(1);
            assertThat(second.invocations.get()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("the async guarantee")
    class AsyncGuarantee {

        @Test
        @DisplayName("publishing does not wait for a handler that blocks indefinitely")
        void publishDoesNotBlockOnAHangingHandler() throws Exception {
            CountDownLatch released = new CountDownLatch(1);
            CountDownLatch entered = new CountDownLatch(1);
            publisher = publisherWith(new BlockingHandler("hanging", entered, released));

            long start = System.nanoTime();
            publisher.publish(event("evt-1"));
            long publishMicros = (System.nanoTime() - start) / 1_000;

            // The handler is inside handle() and will stay there until released.
            assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();

            // publish() returned while the handler is still blocked. The whole
            // in-path budget is 50 ms; a publish that waited on this handler
            // would never return at all.
            assertThat(publishMicros).isLessThan(50_000);

            released.countDown();
        }

        @Test
        @DisplayName("a handler that throws does not propagate to the publisher")
        void handlerFailureIsContained() throws Exception {
            publisher = publisherWith(new ThrowingHandler("broken"));

            // An exception in an async consumer must never become a failed payment.
            publisher.publish(event("evt-1"));
            publisher.awaitQuiescence(5, TimeUnit.SECONDS);

            assertThat(publisher.handlerFailureCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("a handler that is not interested is never invoked")
        void uninterestedHandlerNotInvoked() throws Exception {
            CountingHandler handler = new CountingHandler("counter") {
                @Override
                public boolean handles(String eventType) {
                    return "SomethingElse".equals(eventType);
                }
            };
            publisher = publisherWith(handler);

            publisher.publish(event("evt-1"));
            publisher.awaitQuiescence(5, TimeUnit.SECONDS);

            assertThat(handler.invocations.get()).isZero();
        }
    }

    // --- helpers ------------------------------------------------------------

    private static AsyncEventPublisher publisherWith(EventHandler... handlers) {
        return new AsyncEventPublisher(() -> List.of(handlers), new InMemoryIdempotencyStore());
    }

    private static IprfEvent event(String id) {
        return new TestEvent(id);
    }

    private record TestEvent(String eventId) implements IprfEvent {
        @Override
        public String correlationId() {
            return "corr-1";
        }

        @Override
        public Instant occurredAt() {
            return Instant.parse("2026-08-15T12:00:00Z");
        }

        @Override
        public String eventType() {
            return "TestEvent";
        }
    }

    private static class CountingHandler implements EventHandler {
        final AtomicInteger invocations = new AtomicInteger();
        private final String name;

        CountingHandler(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public boolean handles(String eventType) {
            return true;
        }

        @Override
        public void handle(IprfEvent event) {
            invocations.incrementAndGet();
        }
    }

    private record BlockingHandler(String name, CountDownLatch entered, CountDownLatch released)
            implements EventHandler {

        @Override
        public boolean handles(String eventType) {
            return true;
        }

        @Override
        public void handle(IprfEvent event) {
            entered.countDown();
            try {
                released.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private record ThrowingHandler(String name) implements EventHandler {
        @Override
        public boolean handles(String eventType) {
            return true;
        }

        @Override
        public void handle(IprfEvent event) {
            throw new IllegalStateException("deliberate handler failure");
        }
    }
}
