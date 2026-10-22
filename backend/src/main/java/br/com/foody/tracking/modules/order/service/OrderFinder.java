package br.com.foody.tracking.modules.order.service;

import br.com.foody.tracking.modules.auth.enums.Role;
import br.com.foody.tracking.modules.auth.security.AuthenticatedUser;
import br.com.foody.tracking.modules.order.entity.Order;
import br.com.foody.tracking.modules.order.repository.OrderRepository;
import br.com.foody.tracking.shared.exception.type.OrderNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
final class OrderFinder {

    private final OrderRepository orderRepository;

    Order getVisibleTo(UUID publicId, AuthenticatedUser actor) {
        return (actor.role() == Role.ADMIN
                        ? orderRepository.findByPublicId(publicId)
                        : orderRepository.findByPublicIdAndOwnerPublicId(publicId, actor.id()))
                .orElseThrow(OrderNotFoundException::new);
    }
}
