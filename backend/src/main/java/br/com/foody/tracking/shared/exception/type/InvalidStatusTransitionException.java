package br.com.foody.tracking.shared.exception.type;

import br.com.foody.tracking.shared.exception.model.ApiErrorCode;

// Typed on Enum<?> so shared never imports a module's OrderStatus.
public final class InvalidStatusTransitionException extends DomainException {

  public InvalidStatusTransitionException(Enum<?> from, Enum<?> to) {
    super(
        ApiErrorCode.ORDER_INVALID_TRANSITION,
        "Transição de " + from + " para " + to + " não é permitida");
  }
}
