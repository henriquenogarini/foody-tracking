package br.com.foody.tracking.modules.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.foody.tracking.modules.auth.entity.User;
import br.com.foody.tracking.modules.auth.enums.Role;
import br.com.foody.tracking.modules.auth.repository.UserRepository;
import br.com.foody.tracking.modules.auth.security.AuthenticatedUser;
import br.com.foody.tracking.modules.order.dto.AddressRequest;
import br.com.foody.tracking.modules.order.dto.CreateOrderRequest;
import br.com.foody.tracking.modules.order.dto.CustomerRequest;
import br.com.foody.tracking.modules.order.dto.OrderItemRequest;
import br.com.foody.tracking.modules.order.dto.OrderResponse;
import br.com.foody.tracking.modules.order.dto.UpdateStatusRequest;
import br.com.foody.tracking.modules.order.entity.Customer;
import br.com.foody.tracking.modules.order.entity.DeliveryAddress;
import br.com.foody.tracking.modules.order.entity.Order;
import br.com.foody.tracking.modules.order.entity.OrderItem;
import br.com.foody.tracking.modules.order.entity.OrderStatusHistory;
import br.com.foody.tracking.modules.order.enums.OrderStatus;
import br.com.foody.tracking.modules.order.event.OrderStatusChangedEvent;
import br.com.foody.tracking.modules.order.mapper.OrderMapper;
import br.com.foody.tracking.modules.order.repository.OrderRepository;
import br.com.foody.tracking.shared.exception.type.InvalidStatusTransitionException;
import br.com.foody.tracking.shared.exception.type.InvalidTokenException;
import br.com.foody.tracking.shared.exception.type.OrderNotFoundException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

class OrderServiceTest {

  private static final ZoneId APP_ZONE = ZoneId.of("America/Sao_Paulo");
  private static final Instant NOW = Instant.parse("2026-10-21T17:13:27Z");
  private static final OffsetDateTime CREATED_AT =
      OffsetDateTime.ofInstant(NOW.minusSeconds(3600), APP_ZONE);

  private final OrderRepository orderRepository = mock(OrderRepository.class);
  private final UserRepository userRepository = mock(UserRepository.class);
  private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
  private final OrderService service =
      new OrderService(
          orderRepository,
          new OrderFinder(orderRepository),
          userRepository,
          Mappers.getMapper(OrderMapper.class),
          eventPublisher,
          Clock.fixed(NOW, APP_ZONE));

  private final User owner =
      new User("Ana Souza", "ana@exemplo.com", "hash", Role.USER, CREATED_AT);
  private final AuthenticatedUser ownerActor =
      new AuthenticatedUser(owner.getPublicId(), Role.USER);
  private final AuthenticatedUser adminActor = new AuthenticatedUser(UUID.randomUUID(), Role.ADMIN);

  @Test
  void shouldCreateOrderInRecebidoWithComputedTotal_whenRequestIsValid() {
    // Given
    when(userRepository.findByPublicId(owner.getPublicId())).thenReturn(Optional.of(owner));
    when(orderRepository.nextCode()).thenReturn(1042L);
    when(orderRepository.save(any(Order.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    OrderResponse response = service.create(createRequest(), ownerActor);

    // Then
    ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
    verify(orderRepository).save(saved.capture());
    assertThat(saved.getValue().getOwner()).isSameAs(owner);
    assertThat(saved.getValue().getStatusHistory()).hasSize(1);
    assertThat(response.status()).isEqualTo(OrderStatus.RECEBIDO);
    assertThat(response.code()).isEqualTo(1042L);
    assertThat(response.id()).isEqualTo(saved.getValue().getPublicId());
    assertThat(response.total()).isEqualByComparingTo("128.30");
    assertThat(response.items()).hasSize(2);
    assertThat(response.customer().name()).isEqualTo("José da Silva");
    assertThat(response.deliveryAddress().city()).isEqualTo("São Paulo");
    assertThat(response.createdAt().toInstant()).isEqualTo(NOW);
    assertThat(response.updatedAt().toInstant()).isEqualTo(NOW);
  }

  @Test
  void shouldThrowInvalidToken_whenActorHasNoMatchingUser() {
    // Given
    when(userRepository.findByPublicId(owner.getPublicId())).thenReturn(Optional.empty());

    // When / Then
    assertThatThrownBy(() -> service.create(createRequest(), ownerActor))
        .isInstanceOf(InvalidTokenException.class);
    verify(orderRepository, never()).save(any());
  }

  @Test
  void shouldUseOwnerScopedQuery_whenUserChangesStatus() {
    // Given
    Order order = newOrder();
    when(orderRepository.findByPublicIdAndOwnerPublicId(order.getPublicId(), owner.getPublicId()))
        .thenReturn(Optional.of(order));
    when(orderRepository.saveAndFlush(order)).thenReturn(order);

    // When
    OrderResponse response =
        service.changeStatus(
            order.getPublicId(), new UpdateStatusRequest(OrderStatus.EM_PREPARO), ownerActor);

    // Then
    assertThat(response.status()).isEqualTo(OrderStatus.EM_PREPARO);
    assertThat(response.updatedAt().toInstant()).isEqualTo(NOW);
    verify(orderRepository, never()).findByPublicId(any());
  }

  @Test
  void shouldUseUnscopedQueryAndRecordAdminAsChanger_whenAdminChangesStatus() {
    // Given
    Order order = newOrder();
    when(orderRepository.findByPublicId(order.getPublicId())).thenReturn(Optional.of(order));
    when(orderRepository.saveAndFlush(order)).thenReturn(order);

    // When
    service.changeStatus(
        order.getPublicId(), new UpdateStatusRequest(OrderStatus.CANCELADO), adminActor);

    // Then
    verify(orderRepository, never()).findByPublicIdAndOwnerPublicId(any(), any());
    OrderStatusHistory last = order.getStatusHistory().get(order.getStatusHistory().size() - 1);
    assertThat(last.getFromStatus()).isEqualTo(OrderStatus.RECEBIDO);
    assertThat(last.getToStatus()).isEqualTo(OrderStatus.CANCELADO);
    assertThat(last.getChangedBy()).isEqualTo(adminActor.id());
  }

  @Test
  void shouldThrowOrderNotFound_whenOrderIsOutsideCallerScope() {
    // Given
    UUID publicId = UUID.randomUUID();
    when(orderRepository.findByPublicIdAndOwnerPublicId(publicId, owner.getPublicId()))
        .thenReturn(Optional.empty());

    // When / Then
    assertThatThrownBy(
            () ->
                service.changeStatus(
                    publicId, new UpdateStatusRequest(OrderStatus.EM_PREPARO), ownerActor))
        .isInstanceOf(OrderNotFoundException.class);
    verifyNoInteractions(eventPublisher);
  }

  @Test
  void shouldRejectAndPublishNothing_whenTransitionIsNotAllowed() {
    // Given
    Order order = newOrder();
    when(orderRepository.findByPublicIdAndOwnerPublicId(order.getPublicId(), owner.getPublicId()))
        .thenReturn(Optional.of(order));

    // When / Then
    assertThatThrownBy(
            () ->
                service.changeStatus(
                    order.getPublicId(), new UpdateStatusRequest(OrderStatus.ENTREGUE), ownerActor))
        .isInstanceOf(InvalidStatusTransitionException.class)
        .hasMessage("Transição de RECEBIDO para ENTREGUE não é permitida");
    assertThat(order.getStatus()).isEqualTo(OrderStatus.RECEBIDO);
    verify(orderRepository, never()).saveAndFlush(any());
    verifyNoInteractions(eventPublisher);
  }

  @Test
  void shouldReject_whenTargetIsTheCurrentStatus() {
    // Given
    Order order = newOrder();
    when(orderRepository.findByPublicIdAndOwnerPublicId(order.getPublicId(), owner.getPublicId()))
        .thenReturn(Optional.of(order));

    // When / Then
    assertThatThrownBy(
            () ->
                service.changeStatus(
                    order.getPublicId(), new UpdateStatusRequest(OrderStatus.RECEBIDO), ownerActor))
        .isInstanceOf(InvalidStatusTransitionException.class);
    verifyNoInteractions(eventPublisher);
  }

  @Test
  void shouldPublishStatusChangedEvent_whenTransitionSucceeds() {
    // Given
    Order order = newOrder();
    when(orderRepository.findByPublicIdAndOwnerPublicId(order.getPublicId(), owner.getPublicId()))
        .thenReturn(Optional.of(order));
    when(orderRepository.saveAndFlush(order)).thenReturn(order);

    // When
    service.changeStatus(
        order.getPublicId(), new UpdateStatusRequest(OrderStatus.EM_PREPARO), ownerActor);

    // Then
    ArgumentCaptor<OrderStatusChangedEvent> event =
        ArgumentCaptor.forClass(OrderStatusChangedEvent.class);
    verify(eventPublisher).publishEvent(event.capture());
    assertThat(event.getValue().orderId()).isEqualTo(order.getPublicId());
    assertThat(event.getValue().code()).isEqualTo(1042L);
    assertThat(event.getValue().fromStatus()).isEqualTo(OrderStatus.RECEBIDO);
    assertThat(event.getValue().toStatus()).isEqualTo(OrderStatus.EM_PREPARO);
    assertThat(event.getValue().changedBy()).isEqualTo(owner.getPublicId());
    assertThat(event.getValue().changedAt().toInstant()).isEqualTo(NOW);
  }

  @Test
  void shouldPropagateAndPublishNothing_whenFlushHitsAStaleVersion() {
    // Given
    Order order = newOrder();
    when(orderRepository.findByPublicIdAndOwnerPublicId(order.getPublicId(), owner.getPublicId()))
        .thenReturn(Optional.of(order));
    when(orderRepository.saveAndFlush(order))
        .thenThrow(new ObjectOptimisticLockingFailureException(Order.class, order.getId()));

    // When / Then
    assertThatThrownBy(
            () ->
                service.changeStatus(
                    order.getPublicId(),
                    new UpdateStatusRequest(OrderStatus.EM_PREPARO),
                    ownerActor))
        .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    verifyNoInteractions(eventPublisher);
  }

  @Test
  void shouldStoreNullNotesAndOptionalFields_whenTheyAreBlank() {
    // Given
    when(userRepository.findByPublicId(owner.getPublicId())).thenReturn(Optional.of(owner));
    when(orderRepository.nextCode()).thenReturn(1042L);
    when(orderRepository.save(any(Order.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    CreateOrderRequest request =
        new CreateOrderRequest(
            new CustomerRequest("José da Silva", "", " "),
            List.of(new OrderItemRequest("Pizza calabresa G", 1, new BigDecimal("59.90"))),
            new AddressRequest(
                "Rua José Neves",
                "590",
                "  ",
                "Vila São Paulo",
                "São Paulo",
                "SP",
                "04650-140",
                null,
                null),
            "   ");

    // When
    OrderResponse response = service.create(request, ownerActor);

    // Then
    assertThat(response.notes()).isNull();
    assertThat(response.customer().phone()).isNull();
    assertThat(response.customer().email()).isNull();
    assertThat(response.deliveryAddress().complement()).isNull();
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
        CREATED_AT);
  }

  private static CreateOrderRequest createRequest() {
    return new CreateOrderRequest(
        new CustomerRequest("José da Silva", "+5511987521144", "jose@exemplo.com"),
        List.of(
            new OrderItemRequest("Pizza calabresa G", 2, new BigDecimal("59.90")),
            new OrderItemRequest("Refrigerante", 1, new BigDecimal("8.50"))),
        new AddressRequest(
            "Rua José Neves",
            "590",
            "sala 03",
            "Vila São Paulo",
            "São Paulo",
            "SP",
            "04650-140",
            -23.6578,
            -46.6743),
        "sem cebola");
  }
}
