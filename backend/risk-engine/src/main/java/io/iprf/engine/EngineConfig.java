package io.iprf.engine;

import io.iprf.events.AsyncEventPublisher;
import io.iprf.events.EventHandler;
import io.iprf.events.EventPublisher;
import io.iprf.events.IdempotencyStore;
import io.iprf.events.InMemoryIdempotencyStore;
import io.iprf.state.InMemoryRiskStateStore;
import io.iprf.state.RiskStateStore;
import java.time.Clock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wiring for the decision engine. */
@Configuration
public class EngineConfig {

    /**
     * The clock every engine component reads.
     *
     * <p>Injected rather than called statically so that account-age, staleness
     * and active-hour rules are testable at fixed instants. A rule whose
     * behaviour depends on {@code Instant.now()} cannot be asserted at a
     * threshold boundary, and threshold boundaries are exactly where rules are
     * wrong.
     */
    @Bean
    @ConditionalOnMissingBean
    public Clock iprfClock() {
        return Clock.systemUTC();
    }

    /**
     * The default counterparty risk state store.
     *
     * <p>Registered as a conditional {@code @Bean} rather than annotated
     * {@code @Component}, because {@code @ConditionalOnMissingBean} on a scanned
     * component depends on scan ordering and would silently win or lose the race
     * against the Redis-backed store in the {@code risk-state} module.
     *
     * <p>Keeping a working in-memory default is deliberate: the framework's core
     * loop can be demonstrated by cloning the repository and running one command,
     * with no infrastructure at all.
     */
    @Bean
    @ConditionalOnMissingBean(RiskStateStore.class)
    public InMemoryRiskStateStore inMemoryRiskStateStore(Clock clock) {
        return new InMemoryRiskStateStore(clock);
    }

    @Bean
    @ConditionalOnMissingBean(IdempotencyStore.class)
    public InMemoryIdempotencyStore inMemoryIdempotencyStore() {
        return new InMemoryIdempotencyStore();
    }

    /**
     * The event publisher.
     *
     * <p>Handlers are injected as a list rather than registered by each module,
     * so adding an asynchronous consumer requires only declaring a bean — and,
     * more importantly, so no module can wire itself into the payment path by
     * registering a handler that runs inline.
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(EventPublisher.class)
    public AsyncEventPublisher asyncEventPublisher(
            ObjectProvider<EventHandler> handlers, IdempotencyStore idempotencyStore) {
        // ObjectProvider rather than List: a handler may publish events of its
        // own, which makes the publisher and its handlers mutually dependent at
        // construction. Resolving lazily breaks the cycle.
        return new AsyncEventPublisher(() -> handlers.stream().toList(), idempotencyStore);
    }
}
