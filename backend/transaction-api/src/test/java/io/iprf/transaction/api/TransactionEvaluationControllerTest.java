package io.iprf.transaction.api;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.iprf.state.InMemoryAccountProfileStore;
import io.iprf.state.InMemoryVelocityCounterStore;
import io.iprf.synthetic.SyntheticDatasetGenerator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

/**
 * Contract tests for the decision endpoint.
 *
 * <p>The clock is pinned so the seeded profiles line up with the fixed
 * transaction timestamps used below — otherwise account-age and active-hour
 * rules would drift with wall-clock time and these assertions would fail on a
 * different day.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "iprf.framework-version=test-1.0.0")
class TransactionEvaluationControllerTest {

    private static final Instant NOW = Instant.parse("2026-08-14T14:00:00Z");

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock testClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InMemoryAccountProfileStore profileStore;

    @Autowired
    private InMemoryVelocityCounterStore velocityStore;

    @BeforeEach
    void seedState() {
        profileStore.clear();
        velocityStore.clear();
        profileStore.putAll(new SyntheticDatasetGenerator(20260814).generateProfiles(10, NOW));
    }

    @Test
    @DisplayName("a valid transaction returns a schema-complete decision")
    void returnsSchemaCompleteDecision() throws Exception {
        mockMvc.perform(post("/api/v1/transactions/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Correlation-Id", "test-correlation")
                        .content(body("txn-1", "acct-00000", "acct-payee-1", "125.00")))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", "test-correlation"))
                .andExpect(jsonPath("$.transactionId", is("txn-1")))
                .andExpect(jsonPath("$.correlationId", is("test-correlation")))
                .andExpect(jsonPath("$.decision", notNullValue()))
                .andExpect(jsonPath("$.riskScore",
                        is(allOf(greaterThanOrEqualTo(0.0), lessThanOrEqualTo(1.0)))))
                .andExpect(jsonPath("$.explanation", notNullValue()))
                .andExpect(jsonPath("$.frameworkVersion", is("test-1.0.0")))
                .andExpect(jsonPath("$.evaluatedAt", notNullValue()))
                .andExpect(jsonPath("$.layerResults.IDENTITY_POSTURE").exists())
                .andExpect(jsonPath("$.layerResults.BEHAVIORAL_SCORING").exists());
    }

    @Test
    @DisplayName("latency is measured and present on the decision and every layer")
    void latencyIsMeasured() throws Exception {
        mockMvc.perform(post("/api/v1/transactions/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("txn-2", "acct-00001", "acct-payee-1", "80.00")))
                .andExpect(status().isOk())
                // Measured, not stubbed: a real pipeline always consumes some time.
                .andExpect(jsonPath("$.latencyMicros", is(greaterThan(0))))
                .andExpect(jsonPath("$.layerResults.IDENTITY_POSTURE.latencyMicros",
                        is(greaterThanOrEqualTo(0))))
                .andExpect(jsonPath("$.layerResults.BEHAVIORAL_SCORING.latencyMicros",
                        is(greaterThanOrEqualTo(0))));
    }

    @Test
    @DisplayName("each layer reports its fixed path classification")
    void layersReportPathClassification() throws Exception {
        mockMvc.perform(post("/api/v1/transactions/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("txn-3", "acct-00002", "acct-payee-1", "80.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.layerResults.IDENTITY_POSTURE.path", is("IN_PATH")))
                .andExpect(jsonPath("$.layerResults.BEHAVIORAL_SCORING.path", is("IN_PATH")));
    }

    @Test
    @DisplayName("an unknown payer degrades and is held for review, never allowed")
    void unknownPayerIsReviewedNotAllowed() throws Exception {
        mockMvc.perform(post("/api/v1/transactions/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("txn-4", "acct-does-not-exist", "acct-payee-1", "50.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.degraded", is(true)))
                // The framework's rule: degradation is never a silent approval.
                .andExpect(jsonPath("$.decision", is("REVIEW")))
                .andExpect(jsonPath("$.explanation", containsString("incomplete input")));
    }

    @Test
    @DisplayName("a missing required field is rejected with a field-level violation")
    void rejectsMissingField() throws Exception {
        mockMvc.perform(post("/api/v1/transactions/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"transactionId":"txn-5","payeeAccountId":"acct-payee-1",
                                 "amount":10.00,"currency":"USD","channel":"MOBILE_APP"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.violations", hasSize(1)))
                .andExpect(jsonPath("$.violations[0].field", is("payerAccountId")))
                .andExpect(jsonPath("$.correlationId", notNullValue()));
    }

    @Test
    @DisplayName("a non-positive amount is rejected")
    void rejectsNonPositiveAmount() throws Exception {
        mockMvc.perform(post("/api/v1/transactions/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("txn-6", "acct-00000", "acct-payee-1", "0.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations[0].field", is("amount")));
    }

    @Test
    @DisplayName("a malformed currency is rejected")
    void rejectsMalformedCurrency() throws Exception {
        mockMvc.perform(post("/api/v1/transactions/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"transactionId":"txn-7","payerAccountId":"acct-00000",
                                 "payeeAccountId":"acct-payee-1","amount":10.00,
                                 "currency":"dollars","channel":"MOBILE_APP"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations[0].field", is("currency")));
    }

    @Test
    @DisplayName("an unparseable body is rejected as malformed, not as a server error")
    void rejectsUnparseableBody() throws Exception {
        mockMvc.perform(post("/api/v1/transactions/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ this is not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("MALFORMED_REQUEST")));
    }

    @Test
    @DisplayName("a correlation ID is generated when the caller does not supply one")
    void generatesCorrelationId() throws Exception {
        mockMvc.perform(post("/api/v1/transactions/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("txn-8", "acct-00000", "acct-payee-1", "10.00")))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-Id"))
                .andExpect(jsonPath("$.correlationId", notNullValue()));
    }

    @Test
    @DisplayName("the same input produces the same decision — evaluation is deterministic")
    void isDeterministic() throws Exception {
        String request = body("txn-9", "acct-00003", "acct-payee-1", "95.00");

        String first = mockMvc.perform(post("/api/v1/transactions/evaluate")
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String second = mockMvc.perform(post("/api/v1/transactions/evaluate")
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Latency and timestamp differ by nature; the decision and score must not.
        assertSameField(first, second, "\"decision\":\"");
        assertSameField(first, second, "\"riskScore\":");
    }

    private static void assertSameField(String first, String second, String marker) {
        String a = first.substring(first.indexOf(marker), first.indexOf(marker) + marker.length() + 8);
        String b = second.substring(second.indexOf(marker), second.indexOf(marker) + marker.length() + 8);
        org.assertj.core.api.Assertions.assertThat(a).isEqualTo(b);
    }

    private static String body(String id, String payer, String payee, String amount) {
        return """
                {"transactionId":"%s","payerAccountId":"%s","payeeAccountId":"%s",
                 "amount":%s,"currency":"USD","channel":"MOBILE_APP",
                 "deviceId":"dev-00000-a","rail":"FEDNOW",
                 "initiatedAt":"2026-08-14T14:00:00Z"}""".formatted(id, payer, payee, amount);
    }
}
