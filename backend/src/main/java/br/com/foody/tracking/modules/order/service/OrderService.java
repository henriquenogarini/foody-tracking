package br.com.foody.tracking.modules.order.service;

import br.com.foody.tracking.modules.auth.entity.User;
import br.com.foody.tracking.modules.auth.repository.UserRepository;
import br.com.foody.tracking.modules.auth.security.AuthenticatedUser;
import br.com.foody.tracking.modules.order.dto.CreateOrderRequestDTO;
import br.com.foody.tracking.modules.order.dto.OrderResponseDTO;
import br.com.foody.tracking.modules.order.dto.UpdateStatusRequestDTO;
import br.com.foody.tracking.modules.order.entity.Order;
import br.com.foody.tracking.modules.order.enums.OrderStatus;
import br.com.foody.tracking.modules.order.event.OrderStatusChangedEvent;
import br.com.foody.tracking.modules.order.mapper.OrderMapper;
import br.com.foody.tracking.modules.order.repository.OrderRepository;
import br.com.foody.tracking.shared.exception.type.InvalidStatusTransitionException;
import br.com.foody.tracking.shared.exception.type.InvalidTokenException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Not final: @Transactional needs a CGLIB subclass proxy.
@Service
public class OrderService {

  private static final Logger log = LoggerFactory.getLogger(OrderService.class);

  private final OrderRepository orderRepository;
  private final OrderFinder orderFinder;
  private final UserRepository userRepository;
  private final OrderMapper orderMapper;
  private final ApplicationEventPublisher eventPublisher;
  private final Clock clock;

  OrderService(
      OrderRepository orderRepository,
      OrderFinder orderFinder,
      UserRepository userRepository,
      OrderMapper orderMapper,
      ApplicationEventPublisher eventPublisher,
      Clock clock) {
    this.orderRepository = orderRepository;
    this.orderFinder = orderFinder;
    this.userRepository = userRepository;
    this.orderMapper = orderMapper;
    this.eventPublisher = eventPublisher;
    this.clock = clock;
  }

  @Transactional
  public OrderResponseDTO create(CreateOrderRequestDTO request, AuthenticatedUser actor) {
    // A valid token whose user no longer exists (database reset) is treated as an invalid token.
    User owner = userRepository.findByPublicId(actor.id()).orElseThrow(InvalidTokenException::new);
    // MAX(code)+1 is race-free only because the single-connection pool serializes writers and
    // this read shares the insert's transaction; uq_orders_code remains the last guard.
    long code = orderRepository.nextCode();
    Order order =
        Order.create(
            owner,
            orderMapper.toCustomer(request.customer()),
            request.items().stream().map(orderMapper::toItem).toList(),
            orderMapper.toAddress(request.deliveryAddress()),
            orderMapper.blankToNull(request.notes()),
            code,
            OffsetDateTime.now(clock));
    Order saved = orderRepository.save(order);
    log.info("Order {} created with code {}", saved.getPublicId(), saved.getCode());
    return orderMapper.toResponse(saved);
  }

  @Transactional
  public OrderResponseDTO changeStatus(
      UUID publicId, UpdateStatusRequestDTO request, AuthenticatedUser actor) {
    Order order = orderFinder.findScoped(publicId, actor);
    OrderStatus from = order.getStatus();
    OrderStatus target = request.status();
    OffsetDateTime now = OffsetDateTime.now(clock);
    try {
      order.changeStatus(target, actor.id(), now);
    } catch (InvalidStatusTransitionException ex) {
      log.warn("Order {} transition rejected: {} -> {}", publicId, from, target);
      throw ex;
    }
    // Flushing here makes a stale @Version fail inside this call, so the advice answers 409
    // CONCURRENT_MODIFICATION instead of the commit failing after the controller returned.
    Order saved = orderRepository.saveAndFlush(order);
    // The listener runs AFTER_COMMIT, so a rolled-back change never notifies anyone; it also
    // writes the INFO line for the status change.
    eventPublisher.publishEvent(
        new OrderStatusChangedEvent(
            saved.getPublicId(), saved.getCode(), from, target, actor.id(), now));
    return orderMapper.toResponse(saved);
  }
}
