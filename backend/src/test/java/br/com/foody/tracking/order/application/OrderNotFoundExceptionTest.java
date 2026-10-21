package br.com.foody.tracking.order.application;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.foody.tracking.shared.error.ApiErrorCode;
import br.com.foody.tracking.shared.error.ApiException;
import org.junit.jupiter.api.Test;

class OrderNotFoundExceptionTest {

  @Test
  void shouldFixCodeAndMessage_whenOrderIsNotFound() {
    // When
    ApiException exception = new OrderNotFoundException();

    // Then
    assertThat(exception.getCode()).isEqualTo(ApiErrorCode.ORDER_NOT_FOUND);
    assertThat(exception).hasMessage("Pedido não encontrado.");
  }
}
