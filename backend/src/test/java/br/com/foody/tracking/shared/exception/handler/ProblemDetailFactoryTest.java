package br.com.foody.tracking.shared.exception.handler;

import br.com.foody.tracking.shared.exception.model.ApiErrorCode;
import br.com.foody.tracking.shared.exception.model.ValidationError;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemDetailFactoryTest {

    @Test
    void shouldFillStatusTitleDetailCodeAndUtcTimestamp_whenCreated() {
        // Given
        OffsetDateTime before = OffsetDateTime.now(ZoneOffset.UTC);

        // When
        ProblemDetail problem =
                ProblemDetailFactory.create(ApiErrorCode.ORDER_NOT_FOUND, "Pedido não encontrado");

        // Then
        assertThat(problem.getStatus()).isEqualTo(404);
        assertThat(problem.getTitle()).isEqualTo("Not Found");
        assertThat(problem.getDetail()).isEqualTo("Pedido não encontrado");
        assertThat(problem.getInstance()).isNull();
        assertThat(problem.getProperties()).containsEntry("code", "ORDER_NOT_FOUND");
        OffsetDateTime timestamp = (OffsetDateTime) problem.getProperties().get("timestamp");
        assertThat(timestamp.getOffset()).isEqualTo(ZoneOffset.UTC);
        assertThat(timestamp).isBetween(before, OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Test
    void shouldOmitErrors_whenListIsEmpty() {
        // When
        ProblemDetail problem =
                ProblemDetailFactory.create(
                        ApiErrorCode.VALIDATION_ERROR, "Campos inválidos", List.of());

        // Then
        assertThat(problem.getProperties()).doesNotContainKey("errors");
    }

    @Test
    void shouldIncludeErrors_whenListIsNotEmpty() {
        // Given
        List<ValidationError> errors =
                List.of(new ValidationError("items", "deve conter ao menos 1 item"));

        // When
        ProblemDetail problem =
                ProblemDetailFactory.create(
                        ApiErrorCode.VALIDATION_ERROR, "Campos inválidos", errors);

        // Then
        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getProperties()).containsEntry("errors", errors);
    }
}
