package io.iprf.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The immutable record of one decision, as it is persisted.
 *
 * <p>Deliberately a plain record with no persistence annotations. The in-path
 * modules construct it; the {@code audit} module maps it to storage. Keeping the
 * domain type free of persistence concerns is what allows the ArchUnit guard to
 * forbid JPA imports in {@code risk-engine} without the audit trail becoming
 * impossible to express.
 *
 * <p>Every field exists because reconstructing a decision requires it. In
 * particular {@code ruleVersions} and {@code stateVersionsRead} capture what the
 * system <em>believed</em> at decision time — without them the record describes
 * the decision but cannot reproduce it, because rules and risk state both move
 * on.
 *
 * @param transactionId      the evaluated transaction
 * @param correlationId      request correlation identifier
 * @param frameworkVersion   version of the framework that decided
 * @param decision           the outcome
 * @param riskScore          composite score
 * @param riskFactors        every contributing factor
 * @param ruleVersions       rule id to version, for every rule that executed
 * @param stateVersionsRead  layer to pre-computed state version read
 * @param decidedAt          decision timestamp
 * @param totalLatencyMicros measured in-path duration
 * @param degraded           whether any layer evaluated on incomplete input
 */
public record AuditRecord(
        String transactionId,
        String correlationId,
        String frameworkVersion,
        Decision decision,
        double riskScore,
        List<RiskFactor> riskFactors,
        Map<String, String> ruleVersions,
        Map<ControlLayer, String> stateVersionsRead,
        Instant decidedAt,
        long totalLatencyMicros,
        boolean degraded) {

    public AuditRecord {
        Objects.requireNonNull(transactionId, "transactionId");
        Objects.requireNonNull(decision, "decision");
        Objects.requireNonNull(decidedAt, "decidedAt");
        riskFactors = riskFactors == null ? List.of() : List.copyOf(riskFactors);
        ruleVersions = ruleVersions == null ? Map.of() : Map.copyOf(ruleVersions);
        stateVersionsRead = stateVersionsRead == null ? Map.of() : Map.copyOf(stateVersionsRead);
    }

    /** Builds the audit record for a completed evaluation. */
    public static AuditRecord from(EvaluationResult result) {
        Map<String, String> versions = result.riskFactors().stream()
                .filter(f -> f.ruleId() != null && f.ruleVersion() != null)
                .collect(java.util.stream.Collectors.toMap(
                        RiskFactor::ruleId, RiskFactor::ruleVersion, (a, b) -> a));

        Map<ControlLayer, String> states = result.layerResults().entrySet().stream()
                .filter(e -> e.getValue().stateVersion() != null)
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey, e -> e.getValue().stateVersion()));

        return new AuditRecord(
                result.transactionId(),
                result.correlationId(),
                result.frameworkVersion(),
                result.decision(),
                result.riskScore(),
                result.riskFactors(),
                versions,
                states,
                result.evaluatedAt(),
                result.totalLatencyMicros(),
                result.isDegraded());
    }
}
