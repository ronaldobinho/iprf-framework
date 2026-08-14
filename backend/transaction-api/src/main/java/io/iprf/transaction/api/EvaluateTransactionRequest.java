package io.iprf.transaction.api;

import io.iprf.domain.Channel;
import io.iprf.domain.Rail;
import io.iprf.domain.Transaction;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Request body for {@code POST /api/v1/transactions/evaluate}.
 *
 * <p>Validation is deliberately strict at the boundary. The decision pipeline
 * treats its input as trustworthy — it has to, because re-validating inside each
 * rule would put branching on the latency budget — so the boundary is the only
 * place that check can happen.
 */
@Schema(description = "A payment to be evaluated. SYNTHETIC DATA ONLY.")
public record EvaluateTransactionRequest(

        @Schema(example = "txn-0001", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "transactionId is required")
        @Size(max = 128)
        String transactionId,

        @Schema(example = "acct-payer-001", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "payerAccountId is required")
        @Size(max = 128)
        String payerAccountId,

        @Schema(example = "acct-payee-042", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "payeeAccountId is required")
        @Size(max = 128)
        String payeeAccountId,

        @Schema(example = "1250.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be positive")
        @Digits(integer = 15, fraction = 4, message = "amount has too many digits")
        BigDecimal amount,

        @Schema(example = "USD", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "currency is required")
        @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a 3-letter ISO 4217 code")
        String currency,

        @Schema(example = "MOBILE_APP", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "channel is required")
        Channel channel,

        @Schema(description = "Device identifier, where the channel provides one", example = "dev-a91f")
        @Size(max = 128)
        String deviceId,

        @Schema(description = "Instant-payment rail. Defaults to OTHER.", example = "FEDNOW")
        Rail rail,

        @Schema(description = "When the payer's institution received the order. Defaults to now.")
        Instant initiatedAt) {

    /** Maps to the domain type. Defaults are applied here, not in the pipeline. */
    public Transaction toDomain() {
        return new Transaction(
                transactionId,
                payerAccountId,
                payeeAccountId,
                amount,
                currency,
                channel,
                deviceId,
                rail == null ? Rail.OTHER : rail,
                initiatedAt == null ? Instant.now() : initiatedAt);
    }
}
