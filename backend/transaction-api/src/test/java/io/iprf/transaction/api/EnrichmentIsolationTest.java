package io.iprf.transaction.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.iprf.enrichment.ExternalRiskRegistry;
import io.iprf.state.InMemoryAccountProfileStore;
import io.iprf.synthetic.SyntheticDatasetGenerator;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The structural guarantee that Layer 4 is genuinely off the payment path.
 *
 * <p>The external registry is configured to hang. If enrichment were in-path in
 * any form — a synchronous call, a blocking publish, a shared thread pool the
 * request thread waits on — the endpoint would stall and this test would fail.
 *
 * <p>Reading the code and believing it is not the same as pinning it. This is
 * the test that stops someone "just awaiting" the enrichment future later.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EnrichmentIsolationTest {

    /** The in-path budget from docs/framework/latency-model.md. */
    private static final long IN_PATH_BUDGET_MS = 50;

    private static final CountDownLatch RELEASE = new CountDownLatch(1);
    private static final CountDownLatch ENTERED = new CountDownLatch(1);
    private static final AtomicInteger LOOKUPS = new AtomicInteger();

    @TestConfiguration
    static class HangingRegistryConfig {
        /**
         * A registry that never answers. Modelled on the failure that actually
         * happens: not an error, which is easy to handle, but a dependency that
         * accepts the connection and then goes silent.
         */
        @Bean
        @Primary
        ExternalRiskRegistry hangingRegistry() {
            return accountId -> {
                LOOKUPS.incrementAndGet();
                ENTERED.countDown();
                try {
                    RELEASE.await(30, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return Optional.empty();
            };
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InMemoryAccountProfileStore profileStore;

    @BeforeEach
    void seed() {
        profileStore.clear();
        profileStore.putAll(new SyntheticDatasetGenerator(20260814)
                .generateProfiles(10, Instant.parse("2026-08-15T12:00:00Z")));
    }

    @AfterEach
    void release() {
        RELEASE.countDown();
    }

    @Test
    @DisplayName("decisions are unaffected while the external registry hangs")
    void decisionsAreUnaffectedByAHangingRegistry() throws Exception {
        // Warm the request path so the measurement is not dominated by first-hit
        // class loading and JIT.
        for (int i = 0; i < 5; i++) {
            evaluate("warmup-" + i);
        }

        long slowestMicros = 0;
        for (int i = 0; i < 20; i++) {
            long start = System.nanoTime();
            evaluate("txn-" + i);
            slowestMicros = Math.max(slowestMicros, (System.nanoTime() - start) / 1_000);
        }

        // The registry is inside a lookup and will stay there for 30 seconds.
        assertThat(ENTERED.await(5, TimeUnit.SECONDS))
                .as("the enrichment handler should have reached the registry")
                .isTrue();

        // Every request completed anyway. Generous relative to the 50 ms budget
        // because this measures the full MockMvc stack rather than the pipeline
        // alone — the point is orders of magnitude, not a benchmark.
        assertThat(slowestMicros)
                .as("no request may wait on the hanging registry")
                .isLessThan(IN_PATH_BUDGET_MS * 20 * 1_000);
    }

    @Test
    @DisplayName("the hanging registry is reached asynchronously, not skipped")
    void enrichmentActuallyRuns() throws Exception {
        evaluate("txn-reaches-registry");

        // Guards against the test passing for the wrong reason: if enrichment
        // were disabled or never wired, latency would also be fine and the
        // isolation assertion above would prove nothing.
        assertThat(ENTERED.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(LOOKUPS.get()).isPositive();
    }

    private void evaluate(String transactionId) throws Exception {
        mockMvc.perform(post("/api/v1/transactions/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"transactionId":"%s","payerAccountId":"acct-00000",
                                 "payeeAccountId":"acct-payee-1","amount":100.00,
                                 "currency":"USD","channel":"MOBILE_APP",
                                 "deviceId":"dev-00000-a","rail":"FEDNOW",
                                 "initiatedAt":"2026-08-15T12:00:00Z"}""".formatted(transactionId)))
                .andExpect(status().isOk());
    }
}
