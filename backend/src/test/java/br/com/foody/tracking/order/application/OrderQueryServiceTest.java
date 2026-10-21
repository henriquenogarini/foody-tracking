package br.com.foody.tracking.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import br.com.foody.tracking.auth.application.AuthenticatedUser;
import br.com.foody.tracking.auth.domain.Role;
import br.com.foody.tracking.auth.domain.User;
import br.com.foody.tracking.order.application.dto.OrderResponse;
import br.com.foody.tracking.order.application.dto.OrderSummaryResponse;
import br.com.foody.tracking.order.application.dto.StatusHistoryResponse;
import br.com.foody.tracking.order.domain.Customer;
import br.com.foody.tracking.order.domain.DeliveryAddress;
import br.com.foody.tracking.order.domain.Order;
import br.com.foody.tracking.order.domain.OrderItem;
import br.com.foody.tracking.order.domain.OrderRepository;
import br.com.foody.tracking.order.domain.OrderStatus;
import br.com.foody.tracking.order.domain.OrderSummaryView;
import br.com.foody.tracking.shared.web.InvalidSortException;
import br.com.foody.tracking.shared.web.PageResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class OrderQueryServiceTest {

  private static final ZoneId APP_ZONE = ZoneId.of("America/Sao_Paulo");
  private static final OffsetDateTime NOW =
      OffsetDateTime.ofInstant(Instant.parse("2026-10-21T17:13:27Z"), APP_ZONE);
  private static final Pageable PAGEABLE =
      PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
  // What the repository receives: the client's sort plus the code tie-breaker.
  private static final Pageable STABLE_PAGEABLE =
      PageRequest.of(0, 20, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("code")));

  private final OrderRepository orderRepository = mock(OrderRepository.class);
  private final OrderQueryService service =
      new OrderQueryService(
          orderRepository, new OrderFinder(orderRepository), Mappers.getMapper(OrderMapper.class));

  private final User owner = new User("Ana Souza", "ana@exemplo.com", "hash", Role.USER, NOW);
  private final AuthenticatedUser userActor = new AuthenticatedUser(owner.getPublicId(), Role.USER);
  private final AuthenticatedUser adminActor = new AuthenticatedUser(UUID.randomUUID(), Role.ADMIN);

  @Test
  void shouldUseUnscopedQuery_whenAdminListsWithoutStatus() {
    // Given
    Page<OrderSummaryView> summaries = pageOf(summaryView());
    when(orderRepository.findSummaries(STABLE_PAGEABLE)).thenReturn(summaries);

    // When
    PageResponse<OrderSummaryResponse> page = service.list(adminActor, null, PAGEABLE);

    // Then
    assertThat(page.totalElements()).isEqualTo(1);
    assertThat(page.content().get(0).total()).isEqualByComparingTo("59.90");
    assertThat(page.content().get(0).total().scale()).isEqualTo(2);
    verify(orderRepository).findSummaries(STABLE_PAGEABLE);
    verifyNoMoreInteractions(orderRepository);
  }

  @Test
  void shouldUseUnscopedStatusQuery_whenAdminFiltersByStatus() {
    // Given
    when(orderRepository.findSummariesByStatus(OrderStatus.EM_PREPARO, STABLE_PAGEABLE))
        .thenReturn(pageOf());

    // When
    service.list(adminActor, OrderStatus.EM_PREPARO, PAGEABLE);

    // Then
    verify(orderRepository).findSummariesByStatus(OrderStatus.EM_PREPARO, STABLE_PAGEABLE);
    verifyNoMoreInteractions(orderRepository);
  }

  @Test
  void shouldUseOwnerScopedQuery_whenUserListsWithoutStatus() {
    // Given
    Page<OrderSummaryView> summaries = pageOf(summaryView());
    when(orderRepository.findSummariesByOwnerPublicId(userActor.id(), STABLE_PAGEABLE))
        .thenReturn(summaries);

    // When
    PageResponse<OrderSummaryResponse> page = service.list(userActor, null, PAGEABLE);

    // Then
    assertThat(page.content()).hasSize(1);
    verify(orderRepository).findSummariesByOwnerPublicId(userActor.id(), STABLE_PAGEABLE);
    verifyNoMoreInteractions(orderRepository);
  }

  @Test
  void shouldUseOwnerScopedStatusQuery_whenUserFiltersByStatus() {
    // Given
    when(orderRepository.findSummariesByOwnerPublicIdAndStatus(
            userActor.id(), OrderStatus.ENTREGUE, STABLE_PAGEABLE))
        .thenReturn(pageOf());

    // When
    service.list(userActor, OrderStatus.ENTREGUE, PAGEABLE);

    // Then
    verify(orderRepository)
        .findSummariesByOwnerPublicIdAndStatus(
            userActor.id(), OrderStatus.ENTREGUE, STABLE_PAGEABLE);
    verifyNoMoreInteractions(orderRepository);
  }

  @Test
  void shouldAppendCodeDescending_whenListingWithDefaultSort() {
    // Given
    when(orderRepository.findSummariesByOwnerPublicId(any(), any())).thenReturn(pageOf());

    // When
    service.list(userActor, null, PageRequest.of(2, 10, Sort.by(Sort.Direction.DESC, "createdAt")));

    // Then
    ArgumentCaptor<Pageable> sent = ArgumentCaptor.forClass(Pageable.class);
    verify(orderRepository).findSummariesByOwnerPublicId(eq(userActor.id()), sent.capture());
    assertThat(sent.getValue().getPageNumber()).isEqualTo(2);
    assertThat(sent.getValue().getPageSize()).isEqualTo(10);
    assertThat(sent.getValue().getSort())
        .containsExactly(Sort.Order.desc("createdAt"), Sort.Order.desc("code"));
  }

  @Test
  void shouldKeepClientSort_whenItAlreadyContainsCode() {
    // Given
    Pageable byCodeAscending = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "code"));
    when(orderRepository.findSummaries(any())).thenReturn(pageOf());

    // When
    service.list(adminActor, null, byCodeAscending);

    // Then
    ArgumentCaptor<Pageable> sent = ArgumentCaptor.forClass(Pageable.class);
    verify(orderRepository).findSummaries(sent.capture());
    assertThat(sent.getValue().getSort()).containsExactly(Sort.Order.asc("code"));
  }

  @Test
  void shouldRejectBeforeQuerying_whenSortPropertyIsNotWhitelisted() {
    // Given
    Pageable pageable = PageRequest.of(0, 20, Sort.by("owner.email"));

    // When / Then
    assertThatThrownBy(() -> service.list(userActor, null, pageable))
        .isInstanceOf(InvalidSortException.class);
    verifyNoInteractions(orderRepository);
  }

  @Test
  void shouldReturnOrder_whenUserOwnsIt() {
    // Given
    Order order = newOrder();
    when(orderRepository.findByPublicIdAndOwnerPublicId(order.getPublicId(), userActor.id()))
        .thenReturn(Optional.of(order));

    // When
    OrderResponse response = service.get(order.getPublicId(), userActor);

    // Then
    assertThat(response.id()).isEqualTo(order.getPublicId());
    assertThat(response.total()).isEqualByComparingTo("59.90");
    verify(orderRepository).findByPublicIdAndOwnerPublicId(order.getPublicId(), userActor.id());
    verifyNoMoreInteractions(orderRepository);
  }

  @Test
  void shouldUseUnscopedQuery_whenAdminGetsAnyOrder() {
    // Given
    Order order = newOrder();
    when(orderRepository.findByPublicId(order.getPublicId())).thenReturn(Optional.of(order));

    // When
    OrderResponse response = service.get(order.getPublicId(), adminActor);

    // Then
    assertThat(response.id()).isEqualTo(order.getPublicId());
    verify(orderRepository).findByPublicId(order.getPublicId());
    verifyNoMoreInteractions(orderRepository);
  }

  @Test
  void shouldThrowOrderNotFound_whenUserGetsAnotherUsersOrder() {
    // Given
    UUID publicId = UUID.randomUUID();
    when(orderRepository.findByPublicIdAndOwnerPublicId(publicId, userActor.id()))
        .thenReturn(Optional.empty());

    // When / Then
    assertThatThrownBy(() -> service.get(publicId, userActor))
        .isInstanceOf(OrderNotFoundException.class);
  }

  @Test
  void shouldThrowOrderNotFound_whenUserReadsHistoryOfAnotherUsersOrder() {
    // Given
    UUID publicId = UUID.randomUUID();
    when(orderRepository.findByPublicIdAndOwnerPublicId(publicId, userActor.id()))
        .thenReturn(Optional.empty());

    // When / Then
    assertThatThrownBy(() -> service.history(publicId, userActor))
        .isInstanceOf(OrderNotFoundException.class);
  }

  @Test
  void shouldReturnHistoryInOrder_whenOrderHasTransitions() {
    // Given
    Order order = newOrder();
    order.changeStatus(OrderStatus.EM_PREPARO, owner.getPublicId(), NOW.plusMinutes(5));
    when(orderRepository.findByPublicIdAndOwnerPublicId(order.getPublicId(), userActor.id()))
        .thenReturn(Optional.of(order));

    // When
    List<StatusHistoryResponse> history = service.history(order.getPublicId(), userActor);

    // Then
    assertThat(history)
        .extracting(StatusHistoryResponse::fromStatus, StatusHistoryResponse::toStatus)
        .containsExactly(
            tuple(null, OrderStatus.RECEBIDO), tuple(OrderStatus.RECEBIDO, OrderStatus.EM_PREPARO));
    assertThat(history.get(1).changedBy()).isEqualTo(owner.getPublicId());
  }

  private Order newOrder() {
    return Order.create(
        owner,
        new Customer("José da Silva", null, null),
        List.of(new OrderItem("Pizza calabresa G", 1, new BigDecimal("59.90"))),
        new DeliveryAddress(
            "Rua José Neves",
            "590",
            null,
            "Vila São Paulo",
            "São Paulo",
            "SP",
            "04650-140",
            null,
            null),
        null,
        1042L,
        NOW);
  }

  private static OrderSummaryView summaryView() {
    OrderSummaryView view = mock(OrderSummaryView.class);
    when(view.getPublicId()).thenReturn(UUID.randomUUID());
    when(view.getCode()).thenReturn(1042L);
    when(view.getStatus()).thenReturn(OrderStatus.RECEBIDO);
    when(view.getCustomerName()).thenReturn("José da Silva");
    // SQLite drops the trailing zero; the mapper restores scale 2.
    when(view.getTotal()).thenReturn(new BigDecimal("59.9"));
    when(view.getItemsCount()).thenReturn(1);
    when(view.getCity()).thenReturn("São Paulo");
    when(view.getCreatedAt()).thenReturn(NOW);
    when(view.getUpdatedAt()).thenReturn(NOW);
    return view;
  }

  private static Page<OrderSummaryView> pageOf(OrderSummaryView... views) {
    return new PageImpl<>(List.of(views), STABLE_PAGEABLE, views.length);
  }
}
