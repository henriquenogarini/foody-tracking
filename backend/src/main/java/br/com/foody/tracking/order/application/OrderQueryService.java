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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Not final: @Transactional needs a CGLIB subclass proxy.
@Service
public class OrderQueryService {

  private static final Set<String> SORTABLE_PROPERTIES =
      Set.of("createdAt", "updatedAt", "code", "status", "total");

  // Unique, so equal sort values (same millisecond, status or total) still page deterministically
  // under LIMIT/OFFSET instead of repeating or skipping rows between pages.
  private static final String TIE_BREAKER = "code";

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
    Pageable stable = withTieBreaker(pageable);
    Page<OrderSummaryView> page;
    if (actor.role() == Role.ADMIN) {
      page =
          status == null
              ? orderRepository.findSummaries(stable)
              : orderRepository.findSummariesByStatus(status, stable);
    } else {
      page =
          status == null
              ? orderRepository.findSummariesByOwnerPublicId(actor.id(), stable)
              : orderRepository.findSummariesByOwnerPublicIdAndStatus(actor.id(), status, stable);
    }
    return PageResponse.from(page.map(orderMapper::toSummary));
  }

  private static Pageable withTieBreaker(Pageable pageable) {
    Sort sort = pageable.getSort();
    if (sort.getOrderFor(TIE_BREAKER) != null) {
      return pageable;
    }
    return PageRequest.of(
        pageable.getPageNumber(),
        pageable.getPageSize(),
        sort.and(Sort.by(Sort.Direction.DESC, TIE_BREAKER)));
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
