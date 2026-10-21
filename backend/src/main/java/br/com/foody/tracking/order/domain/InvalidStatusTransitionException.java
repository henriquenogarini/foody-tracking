package br.com.foody.tracking.order.domain;

import br.com.foody.tracking.shared.error.ApiErrorCode;
import br.com.foody.tracking.shared.error.DomainException;

/** Raised by {@link Order#changeStatus} when {@link OrderStatus} forbids the requested move. */
public final class InvalidStatusTransitionException extends DomainException {

  public InvalidStatusTransitionException(OrderStatus from, OrderStatus to) {
    super(
        ApiErrorCode.ORDER_INVALID_TRANSITION,
        "Transição de " + from + " para " + to + " não é permitida");
  }
}
