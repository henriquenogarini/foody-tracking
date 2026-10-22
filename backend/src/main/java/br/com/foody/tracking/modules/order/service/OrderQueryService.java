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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private static final Set<String> SORTABLE_PROPERTIES =
            Set.of("createdAt", "updatedAt", "code", "status", "total");

    private static final String TIE_BREAKER = "code";

    private static final Pattern CODE_QUERY = Pattern.compile("#?(\\d{1,18})");

    private final OrderRepository orderRepository;
    private final OrderFinder orderFinder;
    private final OrderMapper orderMapper;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PageResponseDTO<OrderSummaryResponseDTO> list(
            AuthenticatedUser actor, OrderStatus status, String searchTerm, Pageable pageable) {
        SortWhitelist.requireAllowed(pageable.getSort(), SORTABLE_PROPERTIES);
        Pageable stablePageable = withTieBreaker(pageable);
        Long code = codeOf(searchTerm);
        String namePattern = code == null ? namePatternOf(searchTerm) : null;
        Page<OrderSummaryView> page =
                actor.role() == Role.ADMIN
                        ? orderRepository.searchSummaries(status, code, namePattern, stablePageable)
                        : orderRepository.searchSummariesByOwnerPublicId(
                                actor.id(), status, code, namePattern, stablePageable);
        return PageResponseDTO.from(page.map(orderMapper::toSummaryResponse));
    }

    private static Long codeOf(String searchTerm) {
        if (searchTerm == null) {
            return null;
        }
        Matcher matcher = CODE_QUERY.matcher(searchTerm.strip());
        return matcher.matches() ? Long.valueOf(matcher.group(1)) : null;
    }

    // SQLite's lower() folds only ASCII, so the term is lowercased in Java.
    private static String namePatternOf(String searchTerm) {
        if (searchTerm == null || searchTerm.isBlank()) {
            return null;
        }
        String literal =
                searchTerm
                        .strip()
                        .toLowerCase(Locale.ROOT)
                        .replace("!", "!!")
                        .replace("%", "!%")
                        .replace("_", "!_");
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
        return orderMapper.toResponse(orderFinder.getVisibleTo(publicId, actor));
    }

    @Transactional(readOnly = true)
    public List<StatusHistoryResponseDTO> history(UUID publicId, AuthenticatedUser actor) {
        List<OrderStatusHistory> history =
                orderFinder.getVisibleTo(publicId, actor).getStatusHistory();
        Set<UUID> actorIds =
                history.stream().map(OrderStatusHistory::getChangedBy).collect(Collectors.toSet());
        Map<UUID, String> actorNames =
                userRepository.findByPublicIdIn(actorIds).stream()
                        .collect(
                                Collectors.toMap(UserNameView::getPublicId, UserNameView::getName));
        return orderMapper.toHistoryResponses(history, actorNames);
    }
}
