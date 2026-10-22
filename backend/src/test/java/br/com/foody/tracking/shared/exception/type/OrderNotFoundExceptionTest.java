package br.com.foody.tracking.shared.exception.type;

import br.com.foody.tracking.shared.exception.model.ApiErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
