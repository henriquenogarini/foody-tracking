package br.com.foody.tracking.modules.order.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * Order lifecycle. Anything not listed is invalid, including a move to the same status:
 *
 * <pre>
 * RECEBIDO          -&gt; EM_PREPARO | CANCELADO
 * EM_PREPARO        -&gt; SAIU_PARA_ENTREGA | CANCELADO
 * SAIU_PARA_ENTREGA -&gt; ENTREGUE
 * ENTREGUE          -&gt; (terminal)
 * CANCELADO         -&gt; (terminal)
 * </pre>
 */
public enum OrderStatus {
  RECEBIDO,
  EM_PREPARO,
  SAIU_PARA_ENTREGA,
  ENTREGUE,
  CANCELADO;

  public boolean canTransitionTo(OrderStatus target) {
    return allowedTargets().contains(target);
  }

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
