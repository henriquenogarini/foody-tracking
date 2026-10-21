package br.com.foody.tracking.shared.exception.type;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.foody.tracking.shared.exception.model.ApiErrorCode;
import org.junit.jupiter.api.Test;

class ApiExceptionTest {

  @Test
  void shouldExposeCodeAndMessage_whenCreated() {
    // When
    ApiException exception =
        new ApiException(ApiErrorCode.ORDER_NOT_FOUND, "Pedido não encontrado");

    // Then
    assertThat(exception.getCode()).isEqualTo(ApiErrorCode.ORDER_NOT_FOUND);
    assertThat(exception.getMessage()).isEqualTo("Pedido não encontrado");
  }

  @Test
  void shouldCarryCodeAsApiException_whenDomainExceptionIsThrown() {
    // When
    DomainException exception = new SampleRuleViolation("Transição inválida");

    // Then
    assertThat(exception)
        .isInstanceOf(ApiException.class)
        .hasMessage("Transição inválida")
        .extracting(ApiException::getCode)
        .isEqualTo(ApiErrorCode.ORDER_INVALID_TRANSITION);
  }

  private static final class SampleRuleViolation extends DomainException {

    SampleRuleViolation(String message) {
      super(ApiErrorCode.ORDER_INVALID_TRANSITION, message);
    }
  }
}
