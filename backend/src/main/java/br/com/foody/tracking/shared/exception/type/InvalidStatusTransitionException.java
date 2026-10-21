package br.com.foody.tracking.shared.exception.type;

import br.com.foody.tracking.shared.exception.model.ApiErrorCode;

/**
 * Raised by {@code Order.changeStatus} when {@code OrderStatus} forbids the requested move. Typed
 * on {@code Enum<?>} so {@code shared} never depends on a module; the message uses the constant
 * names exactly as before.
 */
public final class InvalidStatusTransitionException extends DomainException {

  public InvalidStatusTransitionException(Enum<?> from, Enum<?> to) {
    super(
        ApiErrorCode.ORDER_INVALID_TRANSITION,
        "Transição de " + from + " para " + to + " não é permitida");
  }
}
