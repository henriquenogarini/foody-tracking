package br.com.foody.tracking.modules.order.service;

import br.com.foody.tracking.modules.auth.enums.Role;
import br.com.foody.tracking.modules.auth.repository.UserNameView;
import br.com.foody.tracking.modules.auth.repository.UserRepository;
import br.com.foody.tracking.modules.auth.security.AuthenticatedUser;
import br.com.foody.tracking.modules.order.dto.OrderResponseDTO;
import br.com.foody.tracking.modules.order.dto.OrderSummaryResponseDTO;
import br.com.foody.tracking.modules.order.dto.StatusHistoryResponseDTO;
import br.com.foody.tracking.modules.order.entity.OrderStatusHistory;
import br.com.foody.tracking.modules.order.enums.OrderStatus;
import br.com.foody.tracking.modules.order.mapper.OrderMapper;
import br.com.foody.tracking.modules.order.repository.OrderRepository;
import br.com.foody.tracking.modules.order.repository.OrderSummaryView;
import br.com.foody.tracking.shared.web.PageResponseDTO;
import br.com.foody.tracking.shared.web.SortWhitelist;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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

  private static final Pattern CODE_QUERY = Pattern.compile("#?(\\d{1,18})");

  private final OrderRepository orderRepository;
  private final OrderFinder orderFinder;
  private final OrderMapper orderMapper;
  private final UserRepository userRepository;

  OrderQueryService(
      OrderRepository orderRepository,
      OrderFinder orderFinder,
      OrderMapper orderMapper,
      UserRepository userRepository) {
    this.orderRepository = orderRepository;
    this.orderFinder = orderFinder;
    this.orderMapper = orderMapper;
    this.userRepository = userRepository;
  }

  // The role scope is the choice of query: ADMIN reads every order, USER only their own. The
  // search term becomes either a code or a name pattern, never both; null disables a filter.
  @Transactional(readOnly = true)
  public PageResponseDTO<OrderSummaryResponseDTO> list(
      AuthenticatedUser actor, OrderStatus status, String q, Pageable pageable) {
    SortWhitelist.requireAllowed(pageable.getSort(), SORTABLE_PROPERTIES);
    Pageable stable = withTieBreaker(pageable);
    Long code = codeOf(q);
    String namePattern = code == null ? namePatternOf(q) : null;
    Page<OrderSummaryView> page =
        actor.role() == Role.ADMIN
            ? orderRepository.searchSummaries(status, code, namePattern, stable)
            : orderRepository.searchSummariesByOwnerPublicId(
                actor.id(), status, code, namePattern, stable);
    return PageResponseDTO.from(page.map(orderMapper::toSummary));
  }

  // "#1042", "1042" or " #1042 ". At most 18 digits always fits a long; a longer number falls
  // through to the name search, where it matches nothing, instead of failing to parse.
  private static Long codeOf(String q) {
    if (q == null) {
      return null;
    }
    Matcher matcher = CODE_QUERY.matcher(q.strip());
    return matcher.matches() ? Long.valueOf(matcher.group(1)) : null;
  }

  // Lowercased here because SQLite's lower() folds only ASCII: lowering the term in Java at least
  // matches accented names stored in lowercase. LIKE wildcards typed by the user are escaped.
  private static String namePatternOf(String q) {
    if (q == null || q.isBlank()) {
      return null;
    }
    String literal =
        q.strip().toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_");
    return "%" + literal + "%";
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

  // Fixed cost whatever the history length: the order, its history rows, and one query for the
  // distinct actors' current names (no per-row user lookup).
  @Transactional(readOnly = true)
  public List<StatusHistoryResponseDTO> history(UUID publicId, AuthenticatedUser actor) {
    List<OrderStatusHistory> history = orderFinder.findScoped(publicId, actor).getStatusHistory();
    Set<UUID> actorIds =
        history.stream().map(OrderStatusHistory::getChangedBy).collect(Collectors.toSet());
    Map<UUID, String> actorNames =
        userRepository.findByPublicIdIn(actorIds).stream()
            .collect(Collectors.toMap(UserNameView::getPublicId, UserNameView::getName));
    return orderMapper.toHistory(history, actorNames);
  }
}
