package br.com.foody.tracking.shared.exception.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;

// Documentation-only schema, never instantiated: errors are ProblemDetail instances, and springdoc
// cannot see the contract extensions stored in ProblemDetail.properties (code, timestamp, errors).
@Schema(
    name = "ApiErrorResponse",
    description = "Error body: RFC 9457 ProblemDetail plus the contract extensions")
public record ApiErrorResponse(
    @Schema(description = "Problem type URI", example = "about:blank") String type,
    @Schema(description = "Short summary of the HTTP status", example = "Conflict") String title,
    @Schema(description = "HTTP status code", example = "409") int status,
    @Schema(
            description = "Human-readable explanation (Portuguese)",
            example = "Transição de SAIU_PARA_ENTREGA para CANCELADO não é permitida")
        String detail,
    @Schema(
            description = "Request path that produced the error",
            example = "/api/v1/orders/3f6c1a2e-8d4b-4c7a-9e1f-2b5d7a9c0e11/status")
        String instance,
    @Schema(
            description =
                "Stable machine-readable error code, present on every error. Contract codes:"
                    + " VALIDATION_ERROR, AUTH_INVALID_CREDENTIALS, AUTH_TOKEN_INVALID,"
                    + " ACCESS_DENIED, ORDER_NOT_FOUND, USER_EMAIL_ALREADY_EXISTS,"
                    + " ORDER_INVALID_TRANSITION, CONCURRENT_MODIFICATION, INTERNAL_ERROR."
                    + " Framework-built errors: NOT_FOUND (unknown route), METHOD_NOT_ALLOWED,"
                    + " NOT_ACCEPTABLE, UNSUPPORTED_MEDIA_TYPE",
            example = "ORDER_INVALID_TRANSITION")
        ApiErrorCode code,
    @Schema(
            description = "Instant of the error in the application zone (America/Sao_Paulo)",
            example = "2026-10-20T14:00:00-03:00")
        OffsetDateTime timestamp,
    @Schema(
            description = "Field errors; present only on VALIDATION_ERROR",
            example = "[{\"field\":\"items\",\"message\":\"deve conter ao menos 1 item\"}]")
        List<ValidationError> errors) {}
