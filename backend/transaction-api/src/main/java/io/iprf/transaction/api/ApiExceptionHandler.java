package io.iprf.transaction.api;

import io.iprf.transaction.web.CorrelationIdFilter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns validation and parse failures into structured, correlated responses.
 *
 * <p>Every error carries the correlation ID, so a caller reporting a rejected
 * request gives the operator enough to find the exact log line.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @Schema(description = "A rejected request")
    public record ApiError(
            String error,
            String message,
            List<FieldViolation> violations,
            String correlationId,
            Instant timestamp) {
    }

    @Schema(description = "One field that failed validation")
    public record FieldViolation(String field, String message, Object rejectedValue) {
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(ApiExceptionHandler::toViolation)
                .sorted(Comparator.comparing(FieldViolation::field))
                .toList();

        log.info("rejected request: {} validation violation(s): {}",
                violations.size(), violations.stream().map(FieldViolation::field).toList());

        return ResponseEntity.badRequest().body(new ApiError(
                "VALIDATION_FAILED",
                "The request did not pass validation and was not evaluated.",
                violations,
                CorrelationIdFilter.current(request),
                Instant.now()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        log.info("rejected request: unreadable body ({})", ex.getMostSpecificCause().getMessage());

        return ResponseEntity.badRequest().body(new ApiError(
                "MALFORMED_REQUEST",
                "The request body could not be parsed. Check JSON syntax and enum values.",
                List.of(),
                CorrelationIdFilter.current(request),
                Instant.now()));
    }

    /**
     * Anything unhandled. The message is deliberately generic — an internal
     * exception message is an information disclosure risk on a public endpoint,
     * and the correlation ID gives the operator everything needed to find the
     * real one in the logs.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        String correlationId = CorrelationIdFilter.current(request);
        log.error("unhandled exception for correlationId={}", correlationId, ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiError(
                "INTERNAL_ERROR",
                "The request could not be completed.",
                List.of(),
                correlationId,
                Instant.now()));
    }

    private static FieldViolation toViolation(FieldError error) {
        return new FieldViolation(
                error.getField(),
                error.getDefaultMessage(),
                error.getRejectedValue());
    }
}
