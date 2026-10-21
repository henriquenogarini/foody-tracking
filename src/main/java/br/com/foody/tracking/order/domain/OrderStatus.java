package br.com.foody.tracking.order.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Lifecycle of an order and the single owner of its transition matrix. Anything not listed below is
 * invalid, including a transition to the same status:
 *
 * <pre>
 * RECEBIDO          -&gt; EM_PREPARO | CANCELADO
 * EM_PREPARO        -&gt; SAIU_PARA_ENTREGA | CANCELADO
 * SAIU_PARA_ENTREGA -&gt; ENTREGUE
 * ENTREGUE          -&gt; (terminal)
 * CANCELADO         -&gt; (terminal)
 * </pre>
 *
 * <p>The values stay in Portuguese because they are part of the test statement and of the API.
 */
public enum OrderStatus {
  RECEBIDO,
  EM_PREPARO,
  SAIU_PARA_ENTREGA,
  ENTREGUE,
  CANCELADO;

  /** Whether the matrix above allows moving from this status to {@code target}. */
  public boolean canTransitionTo(OrderStatus target) {
    return allowedTargets().contains(target);
  }

  /** Whether no transition leaves this status. */
  public boolean isTerminal() {
    return allowedTargets().isEmpty();
  }

  private Set<OrderStatus> allowedTargets() {
    return switch (this) {
      case RECEBIDO -> EnumSet.of(EM_PREPARO, CANCELADO);
      case EM_PREPARO -> EnumSet.of(SAIU_PARA_ENTREGA, CANCELADO);
      case SAIU_PARA_ENTREGA -> EnumSet.of(ENTREGUE);
      case ENTREGUE, CANCELADO -> EnumSet.noneOf(OrderStatus.class);
    };
  }
}
