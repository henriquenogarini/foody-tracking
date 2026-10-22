package br.com.foody.tracking.shared.exception.type;

import br.com.foody.tracking.shared.exception.model.ApiErrorCode;

public final class OrderNotFoundException extends ApiException {

    public OrderNotFoundException() {
        super(ApiErrorCode.ORDER_NOT_FOUND, "Pedido não encontrado.");
    }
}
