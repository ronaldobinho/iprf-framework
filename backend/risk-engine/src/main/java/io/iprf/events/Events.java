package io.iprf.events;

import io.iprf.domain.CounterpartyRiskTier;
import io.iprf.domain.Decision;
import io.iprf.domain.NetworkFlag;
import io.iprf.domain.SettledTransaction;
import io.iprf.domain.Transaction;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The event catalog.
 *
 * <p>Nine events, as specified in {@code docs/framework/architecture.md}. Two of
 * them carry the framework's feedback loop: {@link TransactionSettled} triggers
 * Layer 5 analysis, and {@link FraudPatternDetected} carries its findings back
 * into pre-computed state for Layer 3 to read. Everything else is supporting
 * traffic.
 *
 * <p>Grouped as nested records rather than scattered across a package because
 * the catalog is a single artifact: adding an event should mean editing one file
 * that a reader can hold in their head.
 */
public final class Events {

    private Events() {
    }

    /** A payment arrived and is about to be evaluated. */
    public record TransactionReceived(
            String eventId, String correlationId, Instant occurredAt,
            Transaction transaction) implements IprfEvent {

        public TransactionReceived(String correlationId, Transaction transaction, Instant at) {
            this(newId(), correlationId, at, transaction);
        }

        @Override
        public String eventType() {
            return "TransactionReceived";
        }
    }

    /**
     * A decision was made.
     *
     * <p>Published after the response has already been returned to the caller.
     * Layer 4 consumes this to enrich the counterparty for <em>future</em>
     * decisions — never for this one.
     */
    public record RiskEvaluationCompleted(
            String eventId, String correlationId, Instant occurredAt,
            String transactionId, String payerAccountId, String payeeAccountId,
            Decision decision, double riskScore, String frameworkVersion,
            long latencyMicros) implements IprfEvent {

        @Override
        public String eventType() {
            return "RiskEvaluationCompleted";
        }
    }

    /** Outcome events. Separate types rather than one event with a field, so a
     *  consumer can subscribe to declines alone without filtering the firehose. */
    public record TransactionApproved(
            String eventId, String correlationId, Instant occurredAt,
            String transactionId) implements IprfEvent {

        @Override
        public String eventType() {
            return "TransactionApproved";
        }
    }

    public record TransactionReviewed(
            String eventId, String correlationId, Instant occurredAt,
            String transactionId, double riskScore) implements IprfEvent {

        @Override
        public String eventType() {
            return "TransactionReviewed";
        }
    }

    public record TransactionDeclined(
            String eventId, String correlationId, Instant occurredAt,
            String transactionId, double riskScore) implements IprfEvent {

        @Override
        public String eventType() {
            return "TransactionDeclined";
        }
    }

    /**
     * A payment settled — the money actually moved.
     *
     * <p>Triggers Layer 5. Analysing authorization attempts instead would count
     * declined payments as evidence of a receiver's activity.
     */
    public record TransactionSettled(
            String eventId, String correlationId, Instant occurredAt,
            SettledTransaction settled) implements IprfEvent {

        public TransactionSettled(String correlationId, SettledTransaction settled) {
            this(newId(), correlationId, settled.settledAt(), settled);
        }

        @Override
        public String eventType() {
            return "TransactionSettled";
        }
    }

    /** Layer 4 obtained external intelligence and updated pre-computed state. */
    public record ExternalRiskUpdated(
            String eventId, String correlationId, Instant occurredAt,
            String counterpartyId, CounterpartyRiskTier tier,
            List<String> typologies, long stateVersion) implements IprfEvent {

        @Override
        public String eventType() {
            return "ExternalRiskUpdated";
        }
    }

    /**
     * Layer 5 detected a typology.
     *
     * <p>The other half of the feedback loop. Carries the evidence, not just the
     * verdict — a tier raised without justification degrades every future
     * decision about that account with no way to tell a real finding from a
     * detector misfiring.
     */
    public record FraudPatternDetected(
            String eventId, String correlationId, Instant occurredAt,
            String accountId, NetworkFlag flag, Set<NetworkFlag> allFlags,
            CounterpartyRiskTier tier, String typology, String evidence,
            int observations) implements IprfEvent {

        @Override
        public String eventType() {
            return "FraudPatternDetected";
        }
    }

    /** An institutional assessment run finished. */
    public record AssessmentCompleted(
            String eventId, String correlationId, Instant occurredAt,
            String assessmentId, String institution, int overallLevel,
            String modelVersion) implements IprfEvent {

        @Override
        public String eventType() {
            return "AssessmentCompleted";
        }
    }

    static String newId() {
        return UUID.randomUUID().toString();
    }
}
