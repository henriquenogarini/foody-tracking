package br.com.foody.tracking.order.application;

import br.com.foody.tracking.auth.application.AuthenticatedUser;
import br.com.foody.tracking.auth.domain.Role;
import br.com.foody.tracking.order.application.dto.OrderResponse;
import br.com.foody.tracking.order.application.dto.OrderSummaryResponse;
import br.com.foody.tracking.order.application.dto.StatusHistoryResponse;
import br.com.foody.tracking.order.domain.OrderRepository;
import br.com.foody.tracking.order.domain.OrderStatus;
import br.com.foody.tracking.order.domain.OrderSummaryView;
import br.com.foody.tracking.shared.web.PageResponse;
import br.com.foody.tracking.shared.web.SortWhitelist;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Not final: @Transactional needs a CGLIB subclass proxy.
@Service
public class OrderQueryService {

  private static final Set<String> SORTABLE_PROPERTIES =
      Set.of("createdAt", "updatedAt", "code", "status", "total");

  private final OrderRepository orderRepository;
  private final OrderFinder orderFinder;
  private final OrderMapper orderMapper;

  OrderQueryService(
      OrderRepository orderRepository, OrderFinder orderFinder, OrderMapper orderMapper) {
    this.orderRepository = orderRepository;
    this.orderFinder = orderFinder;
    this.orderMapper = orderMapper;
  }

  // The role scope is the choice of query: ADMIN reads every order, USER only their own.
  @Transactional(readOnly = true)
  public PageResponse<OrderSummaryResponse> list(
      AuthenticatedUser actor, OrderStatus status, Pageable pageable) {
    SortWhitelist.requireAllowed(pageable.getSort(), SORTABLE_PROPERTIES);
    Page<OrderSummaryView> page;
    if (actor.role() == Role.ADMIN) {
      page =
          status == null
              ? orderRepository.findSummaries(pageable)
              : orderRepository.findSummariesByStatus(status, pageable);
    } else {
      page =
          status == null
              ? orderRepository.findSummariesByOwnerPublicId(actor.id(), pageable)
              : orderRepository.findSummariesByOwnerPublicIdAndStatus(actor.id(), status, pageable);
    }
    return PageResponse.from(page.map(orderMapper::toSummary));
  }

  @Transactional(readOnly = true)
  public OrderResponse get(UUID publicId, AuthenticatedUser actor) {
    return orderMapper.toResponse(orderFinder.findScoped(publicId, actor));
  }

  @Transactional(readOnly = true)
  public List<StatusHistoryResponse> history(UUID publicId, AuthenticatedUser actor) {
    return orderMapper.toHistory(orderFinder.findScoped(publicId, actor).getStatusHistory());
  }
}
