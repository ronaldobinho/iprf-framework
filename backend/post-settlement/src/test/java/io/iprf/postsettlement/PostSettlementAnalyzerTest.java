package io.iprf.postsettlement;

import static org.assertj.core.api.Assertions.assertThat;

import io.iprf.domain.Channel;
import io.iprf.domain.CounterpartyRiskTier;
import io.iprf.domain.NetworkFlag;
import io.iprf.domain.Rail;
import io.iprf.domain.SettledTransaction;
import io.iprf.domain.Transaction;
import io.iprf.postsettlement.detectors.FanInDetector;
import io.iprf.postsettlement.detectors.FanOutDetector;
import io.iprf.postsettlement.detectors.StructuringDetector;
import io.iprf.state.CounterpartyRiskState;
import io.iprf.state.InMemoryRiskStateStore;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PostSettlementAnalyzerTest {

    private static final Instant NOW = Instant.parse("2026-08-15T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private InMemoryRiskStateStore state;
    private PostSettlementAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        state = new InMemoryRiskStateStore(CLOCK);
        PostSettlementProperties properties = properties();
        analyzer = new PostSettlementAnalyzer(
                List.of(
                        new FanInDetector(properties),
                        new FanOutDetector(properties),
                        new StructuringDetector(properties)),
                state);
    }

    @Nested
    @DisplayName("fan-in — the mule collection signature")
    class FanIn {

        @Test
        @DisplayName("distinct payers at the threshold raise the receiver's tier")
        void firesAtThreshold() {
            List<SettledTransaction> window = paymentsTo("acct-mule", 5, "100.00");

            List<PatternDetection> detections = analyzer.analyse(window);

            assertThat(detections).hasSize(1);
            PatternDetection detection = detections.get(0);
            assertThat(detection.accountId()).isEqualTo("acct-mule");
            assertThat(detection.flag()).isEqualTo(NetworkFlag.FAN_IN);
            assertThat(detection.tier()).isEqualTo(CounterpartyRiskTier.ELEVATED);
            assertThat(detection.evidence()).contains("5 distinct payers");
        }

        @Test
        @DisplayName("one payer under the threshold does not fire")
        void doesNotFireBelowThreshold() {
            assertThat(analyzer.analyse(paymentsTo("acct-shop", 4, "100.00"))).isEmpty();
        }

        @Test
        @DisplayName("many payments from ONE payer are not a fan-in pattern")
        void countsDistinctPayersNotTransactions() {
            List<SettledTransaction> window = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                window.add(settled("acct-payer-single", "acct-shop", "100.00"));
            }

            // Twenty payments from the same person is a subscription, not a mule.
            assertThat(analyzer.analyse(window)).isEmpty();
        }

        @Test
        @DisplayName("well past the threshold escalates to HIGH rather than ELEVATED")
        void rampsWithSeverity() {
            List<PatternDetection> detections = analyzer.analyse(paymentsTo("acct-mule", 15, "100.00"));

            assertThat(detections.get(0).tier()).isEqualTo(CounterpartyRiskTier.HIGH);
        }

        @Test
        @DisplayName("self-transfers are not fan-in contributors")
        void ignoresSelfTransfers() {
            List<SettledTransaction> window = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                window.add(settled("acct-same", "acct-same", "100.00"));
            }

            assertThat(analyzer.analyse(window)).isEmpty();
        }
    }

    @Nested
    @DisplayName("structuring")
    class Structuring {

        @Test
        @DisplayName("repeated amounts just below a threshold fire")
        void firesJustBelowThreshold() {
            List<SettledTransaction> window = List.of(
                    settled("acct-structurer", "acct-a", "9800.00"),
                    settled("acct-structurer", "acct-b", "9750.00"),
                    settled("acct-structurer", "acct-c", "9900.00"));

            List<PatternDetection> detections = analyzer.analyse(window).stream()
                    .filter(d -> d.flag() == NetworkFlag.STRUCTURING)
                    .toList();

            assertThat(detections).hasSize(1);
            assertThat(detections.get(0).evidence()).contains("just below 10000.00");
        }

        @Test
        @DisplayName("an amount exactly at the threshold is not evasion")
        void exactlyAtThresholdIsNotStructuring() {
            List<SettledTransaction> window = List.of(
                    settled("acct-payer", "acct-a", "10000.00"),
                    settled("acct-payer", "acct-b", "10000.00"),
                    settled("acct-payer", "acct-c", "10000.00"));

            // These payments would have triggered the control, not evaded it.
            assertThat(analyzer.analyse(window).stream()
                    .filter(d -> d.flag() == NetworkFlag.STRUCTURING)).isEmpty();
        }

        @Test
        @DisplayName("amounts well below a threshold are not near it")
        void wellBelowIsNotStructuring() {
            List<SettledTransaction> window = List.of(
                    settled("acct-payer", "acct-a", "500.00"),
                    settled("acct-payer", "acct-b", "600.00"),
                    settled("acct-payer", "acct-c", "700.00"));

            assertThat(analyzer.analyse(window).stream()
                    .filter(d -> d.flag() == NetworkFlag.STRUCTURING)).isEmpty();
        }
    }

    @Nested
    @DisplayName("the feedback loop into risk state")
    class FeedbackLoop {

        @Test
        @DisplayName("a detection writes a tier, a flag and a typology that Layer 3 can read")
        void writesReadableState() {
            analyzer.analyse(paymentsTo("acct-mule", 6, "100.00"));

            CounterpartyRiskState written = state.findByCounterpartyId("acct-mule").orElseThrow();
            assertThat(written.tier()).isEqualTo(CounterpartyRiskTier.ELEVATED);
            assertThat(written.flags()).contains(NetworkFlag.FAN_IN);
            assertThat(written.reportedTypologies()).contains("MULE_COLLECTION_FAN_IN");
            assertThat(written.version()).isPositive();
        }

        @Test
        @DisplayName("a weaker later detection does not lower a tier already set")
        void tiersOnlyRise() {
            // First: a strong signal.
            analyzer.analyse(paymentsTo("acct-mule", 15, "100.00"));
            assertThat(state.findByCounterpartyId("acct-mule").orElseThrow().tier())
                    .isEqualTo(CounterpartyRiskTier.HIGH);

            // Then: a weaker one, from a window that happens to contain less.
            analyzer.analyse(paymentsTo("acct-mule", 5, "100.00"));

            // Clearing risk is a deliberate operation, not a side effect of a
            // detector running with different inputs.
            assertThat(state.findByCounterpartyId("acct-mule").orElseThrow().tier())
                    .isEqualTo(CounterpartyRiskTier.HIGH);
        }

        @Test
        @DisplayName("the state version advances on each write, so decisions can cite it")
        void versionAdvances() {
            analyzer.analyse(paymentsTo("acct-mule", 6, "100.00"));
            long first = state.findByCounterpartyId("acct-mule").orElseThrow().version();

            analyzer.analyse(paymentsTo("acct-mule", 6, "100.00"));
            long second = state.findByCounterpartyId("acct-mule").orElseThrow().version();

            assertThat(second).isGreaterThan(first);
        }

        @Test
        @DisplayName("an empty window produces nothing and writes nothing")
        void emptyWindowIsSafe() {
            assertThat(analyzer.analyse(List.of())).isEmpty();
            assertThat(state.size()).isZero();
        }
    }

    // --- helpers ------------------------------------------------------------

    private static List<SettledTransaction> paymentsTo(String receiver, int distinctPayers, String amount) {
        List<SettledTransaction> window = new ArrayList<>();
        for (int i = 0; i < distinctPayers; i++) {
            window.add(settled("acct-payer-%02d".formatted(i), receiver, amount));
        }
        return window;
    }

    private static SettledTransaction settled(String payer, String payee, String amount) {
        return new SettledTransaction(
                new Transaction(
                        "txn-" + payer + "-" + payee + "-" + amount,
                        payer, payee, new BigDecimal(amount), "USD",
                        Channel.MOBILE_APP, "dev-1", Rail.FEDNOW, NOW),
                NOW);
    }

    private static PostSettlementProperties properties() {
        return new PostSettlementProperties(
                24, 5, 8, 3,
                List.of(new BigDecimal("10000.00"), new BigDecimal("5000.00")),
                0.05);
    }
}
