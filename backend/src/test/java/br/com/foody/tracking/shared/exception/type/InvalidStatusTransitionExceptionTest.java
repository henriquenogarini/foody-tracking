package br.com.foody.tracking.shared.exception.type;

import static br.com.foody.tracking.modules.order.enums.OrderStatus.CANCELADO;
import static br.com.foody.tracking.modules.order.enums.OrderStatus.SAIU_PARA_ENTREGA;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.foody.tracking.shared.exception.model.ApiErrorCode;
import org.junit.jupiter.api.Test;

class InvalidStatusTransitionExceptionTest {

  @Test
  void shouldCarryCodeAndPortugueseMessage_whenTransitionIsInvalid() {
    // When
    InvalidStatusTransitionException exception =
        new InvalidStatusTransitionException(SAIU_PARA_ENTREGA, CANCELADO);

    // Then
    assertThat(exception).isInstanceOf(DomainException.class);
    assertThat(exception.getCode()).isEqualTo(ApiErrorCode.ORDER_INVALID_TRANSITION);
    assertThat(exception.getMessage())
        .isEqualTo("Transição de SAIU_PARA_ENTREGA para CANCELADO não é permitida");
  }
}
