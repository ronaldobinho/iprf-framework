package io.iprf.enrichment;

import io.iprf.events.EventHandler;
import io.iprf.events.EventPublisher;
import io.iprf.events.Events;
import io.iprf.events.IprfEvent;
import io.iprf.state.RiskStateWriter;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Layer 4 — consumes decision events, queries external intelligence, and writes
 * what it learns into pre-computed state for <b>future</b> decisions.
 *
 * <p>Never touches the decision that triggered it. By the time this runs, that
 * response has already been returned to the caller. The value it adds lands on
 * the next payment to the same counterparty.
 *
 * <p>Failures here are ordinary. A registry timeout, an outage, a rejected
 * lookup — each degrades future decision quality slightly and none of them fail
 * a payment. That asymmetry is the entire reason this layer is asynchronous.
 */
@Component
@ConditionalOnProperty(name = "iprf.enrichment.enabled", havingValue = "true", matchIfMissing = true)
public class ExternalEnrichmentHandler implements EventHandler {

    private static final Logger log = LoggerFactory.getLogger(ExternalEnrichmentHandler.class);

    private final ExternalRiskRegistry registry;
    private final RiskStateWriter riskStateWriter;
    private final EventPublisher eventPublisher;
    private final Clock clock;

    private final AtomicLong enriched = new AtomicLong();
    private final AtomicLong notFound = new AtomicLong();
    private final AtomicLong failures = new AtomicLong();

    public ExternalEnrichmentHandler(
            ExternalRiskRegistry registry,
            RiskStateWriter riskStateWriter,
            EventPublisher eventPublisher,
            Clock clock) {
        this.registry = registry;
        this.riskStateWriter = riskStateWriter;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    public String name() {
        return "external-enrichment";
    }

    @Override
    public boolean handles(String eventType) {
        return "RiskEvaluationCompleted".equals(eventType);
    }

    @Override
    public void handle(IprfEvent event) {
        if (!(event instanceof Events.RiskEvaluationCompleted completed)) {
            return;
        }
        String counterpartyId = completed.payeeAccountId();

        Optional<ExternalRiskRegistry.Report> report;
        try {
            report = registry.lookup(counterpartyId);
        } catch (ExternalRiskRegistry.ExternalRegistryException e) {
            // Existing state keeps its previous version rather than being
            // cleared. A failed lookup must not erase what was already known.
            failures.incrementAndGet();
            log.debug("registry lookup failed counterpartyId={}: {}",
                    counterpartyId, e.getMessage());
            return;
        }

        if (report.isEmpty()) {
            notFound.incrementAndGet();
            return;
        }

        ExternalRiskRegistry.Report found = report.get();
        if (!found.tier().isAdverse() && found.typologies().isEmpty()) {
            // Nothing adverse to record. Writing a LOW tier would be recording
            // an opinion as a finding.
            notFound.incrementAndGet();
            return;
        }

        long version = riskStateWriter.raiseTier(counterpartyId, found.tier(), List.of());
        for (String typology : found.typologies()) {
            version = riskStateWriter.recordTypology(counterpartyId, typology);
        }
        enriched.incrementAndGet();

        eventPublisher.publish(new Events.ExternalRiskUpdated(
                UUID.randomUUID().toString(),
                completed.correlationId(),
                clock.instant(),
                counterpartyId,
                found.tier(),
                found.typologies(),
                version));

        log.debug("enriched counterpartyId={} tier={} stateVersion={}",
                counterpartyId, found.tier(), version);
    }

    public long enrichedCount() {
        return enriched.get();
    }

    public long notFoundCount() {
        return notFound.get();
    }

    public long failureCount() {
        return failures.get();
    }
}
