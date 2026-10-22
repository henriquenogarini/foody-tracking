package br.com.foody.tracking.shared.exception.type;

import br.com.foody.tracking.shared.exception.model.ApiErrorCode;

public final class InvalidStatusTransitionException extends DomainException {

  public InvalidStatusTransitionException(Enum<?> from, Enum<?> to) {
    super(
        ApiErrorCode.ORDER_INVALID_TRANSITION,
        "Transição de " + from + " para " + to + " não é permitida");
  }
}
