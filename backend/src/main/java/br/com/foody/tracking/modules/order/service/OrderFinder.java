package br.com.foody.tracking.modules.order.service;

import br.com.foody.tracking.modules.auth.enums.Role;
import br.com.foody.tracking.modules.auth.security.AuthenticatedUser;
import br.com.foody.tracking.modules.order.entity.Order;
import br.com.foody.tracking.modules.order.repository.OrderRepository;
import br.com.foody.tracking.shared.exception.type.OrderNotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Component;

// The single owner of the role scope for one order: ADMIN reads any order, USER only their own.
@Component
final class OrderFinder {

  private final OrderRepository orderRepository;

  OrderFinder(OrderRepository orderRepository) {
    this.orderRepository = orderRepository;
  }

  // No @Transactional: it always runs inside the calling service's transaction (read or write).
  // Another user's order is indistinguishable from a missing one (404, never 403).
  Order getVisibleTo(UUID publicId, AuthenticatedUser actor) {
    return (actor.role() == Role.ADMIN
            ? orderRepository.findByPublicId(publicId)
            : orderRepository.findByPublicIdAndOwnerPublicId(publicId, actor.id()))
        .orElseThrow(OrderNotFoundException::new);
  }
}
