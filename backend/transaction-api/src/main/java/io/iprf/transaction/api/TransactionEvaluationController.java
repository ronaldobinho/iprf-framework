package io.iprf.transaction.api;

import io.iprf.domain.EvaluationResult;
import io.iprf.domain.Transaction;
import io.iprf.events.EventPublisher;
import io.iprf.events.Events;
import io.iprf.engine.DecisionPipeline;
import io.iprf.transaction.web.CorrelationIdFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The decision endpoint.
 *
 * <p>Thin by design: validate, delegate, map. Rule logic lives in
 * {@code risk-engine}. A controller that starts making risk judgements is a
 * controller that has escaped the ArchUnit guard protecting the in-path
 * contract.
 */
@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transaction evaluation",
        description = "Deterministic, explainable fraud decisioning for instant payments")
public class TransactionEvaluationController {

    private final DecisionPipeline pipeline;
    private final EventPublisher eventPublisher;

    public TransactionEvaluationController(DecisionPipeline pipeline, EventPublisher eventPublisher) {
        this.pipeline = pipeline;
        this.eventPublisher = eventPublisher;
    }

    @PostMapping(
            path = "/evaluate",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Evaluate a transaction",
            description = """
                    Runs the in-path control layers and returns a decision of ALLOW, REVIEW or
                    DECLINE together with the composite risk score, every contributing rule with
                    its version, per-layer results and measured latency.

                    Deterministic: the same input under the same rule versions always produces
                    the same output. No live database query is performed during evaluation.

                    All data is SYNTHETIC. This endpoint is a reference implementation, not a
                    production fraud service.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Decision produced"),
            @ApiResponse(responseCode = "400", description = "Request failed validation")
    })
    public EvaluateTransactionResponse evaluate(
            @Valid @RequestBody EvaluateTransactionRequest request,
            HttpServletRequest httpRequest) {

        Transaction transaction = request.toDomain();
        EvaluationResult result = pipeline.evaluate(
                transaction, CorrelationIdFilter.current(httpRequest));

        // Published after the decision is made and before the response is
        // serialized, but the publisher hands off to an executor and returns —
        // nothing downstream of this line can affect the caller's latency. That
        // property is asserted by a test in which the enrichment registry hangs.
        eventPublisher.publish(new Events.RiskEvaluationCompleted(
                UUID.randomUUID().toString(),
                result.correlationId(),
                result.evaluatedAt(),
                result.transactionId(),
                transaction.payerAccountId(),
                transaction.payeeAccountId(),
                result.decision(),
                result.riskScore(),
                result.frameworkVersion(),
                result.totalLatencyMicros()));

        return EvaluateTransactionResponse.from(result);
    }
}
