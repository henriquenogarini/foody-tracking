package br.com.foody.tracking.modules.order.service;

import br.com.foody.tracking.modules.auth.enums.Role;
import br.com.foody.tracking.modules.auth.security.AuthenticatedUser;
import br.com.foody.tracking.modules.order.dto.OrderResponseDTO;
import br.com.foody.tracking.modules.order.dto.OrderSummaryResponseDTO;
import br.com.foody.tracking.modules.order.dto.StatusHistoryResponseDTO;
import br.com.foody.tracking.modules.order.enums.OrderStatus;
import br.com.foody.tracking.modules.order.mapper.OrderMapper;
import br.com.foody.tracking.modules.order.repository.OrderRepository;
import br.com.foody.tracking.modules.order.repository.OrderSummaryView;
import br.com.foody.tracking.shared.web.PageResponseDTO;
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
  public PageResponseDTO<OrderSummaryResponseDTO> list(
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
    return PageResponseDTO.from(page.map(orderMapper::toSummary));
  }

  private static Pageable withTieBreaker(Pageable pageable) {
    if (!pageable.isPaged()) {
      return pageable;
    }
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
  public OrderResponseDTO get(UUID publicId, AuthenticatedUser actor) {
    return orderMapper.toResponse(orderFinder.findScoped(publicId, actor));
  }

  @Transactional(readOnly = true)
  public List<StatusHistoryResponseDTO> history(UUID publicId, AuthenticatedUser actor) {
    return orderMapper.toHistory(orderFinder.findScoped(publicId, actor).getStatusHistory());
  }
}
