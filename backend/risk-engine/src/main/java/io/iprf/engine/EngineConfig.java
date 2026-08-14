package io.iprf.engine;

import java.time.Clock;
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
}
