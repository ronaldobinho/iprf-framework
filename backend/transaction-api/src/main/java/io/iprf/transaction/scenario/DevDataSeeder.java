package io.iprf.transaction.scenario;

import io.iprf.state.AccountProfile;
import io.iprf.state.InMemoryAccountProfileStore;
import io.iprf.synthetic.SyntheticDatasetGenerator;
import java.time.Clock;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Loads synthetic account profiles at startup so the running API produces
 * differentiated decisions.
 *
 * <p>Without seeded profiles every payer is unknown, Layer 1 degrades, and the
 * engine correctly answers {@code REVIEW} to everything — right behaviour, but a
 * demonstration of nothing.
 *
 * <p><b>Loading happens at startup, off the payment path</b>, which is what the
 * in-path contract requires: pre-computed state is materialized before the
 * transaction arrives, never assembled during it.
 *
 * <p>Note the shape of this: startup work whose cost is a function of how many
 * profiles exist. That is the growth-coupling pattern this framework warns
 * about, and it is why Phase 2's Redis-backed store matters — external state
 * makes recovery a reconnection rather than a reload. See
 * {@code docs/framework/growth-coupling.md}.
 */
@Component
@ConditionalOnProperty(name = "iprf.seed.enabled", havingValue = "true")
public class DevDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private final InMemoryAccountProfileStore profileStore;
    private final Clock clock;
    private final int profileCount;
    private final long seed;

    public DevDataSeeder(
            InMemoryAccountProfileStore profileStore,
            Clock clock,
            @Value("${iprf.seed.profiles:200}") int profileCount,
            @Value("${iprf.seed.seed:20260814}") long seed) {
        this.profileStore = profileStore;
        this.clock = clock;
        this.profileCount = profileCount;
        this.seed = seed;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void seed() {
        long start = System.nanoTime();
        List<AccountProfile> profiles =
                new SyntheticDatasetGenerator(seed).generateProfiles(profileCount, clock.instant());
        profileStore.putAll(profiles);

        log.warn("SEEDED {} SYNTHETIC account profiles in {} ms (seed={}). "
                        + "This is DEMO DATA and must never be enabled in a real deployment.",
                profiles.size(), (System.nanoTime() - start) / 1_000_000, seed);
        log.info("Example payer account ids: {}",
                profiles.stream().limit(3).map(AccountProfile::accountId).toList());
    }
}
