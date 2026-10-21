package br.com.foody.tracking.order.application;

import br.com.foody.tracking.shared.error.ApiErrorCode;
import br.com.foody.tracking.shared.error.ApiException;

public final class OrderNotFoundException extends ApiException {

  public OrderNotFoundException() {
    super(ApiErrorCode.ORDER_NOT_FOUND, "Pedido não encontrado.");
  }
}
