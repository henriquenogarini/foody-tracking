package br.com.foody.tracking.modules.order.service;

import br.com.foody.tracking.modules.auth.entity.User;
import br.com.foody.tracking.modules.auth.enums.Role;
import br.com.foody.tracking.modules.auth.repository.UserRepository;
import br.com.foody.tracking.modules.auth.security.AuthenticatedUser;
import br.com.foody.tracking.modules.order.dto.AddressRequestDTO;
import br.com.foody.tracking.modules.order.dto.CreateOrderRequestDTO;
import br.com.foody.tracking.modules.order.dto.CustomerRequestDTO;
import br.com.foody.tracking.modules.order.dto.OrderItemRequestDTO;
import br.com.foody.tracking.modules.order.dto.OrderResponseDTO;
import br.com.foody.tracking.modules.order.dto.UpdateStatusRequestDTO;
import br.com.foody.tracking.modules.order.entity.Order;
import br.com.foody.tracking.modules.order.entity.OrderStatusHistory;
import br.com.foody.tracking.modules.order.enums.OrderStatus;
import br.com.foody.tracking.modules.order.event.OrderStatusChangedEvent;
import br.com.foody.tracking.modules.order.mapper.OrderMapper;
import br.com.foody.tracking.modules.order.repository.OrderRepository;
import br.com.foody.tracking.shared.exception.type.InvalidStatusTransitionException;
import br.com.foody.tracking.shared.exception.type.InvalidTokenException;
import br.com.foody.tracking.shared.exception.type.OrderNotFoundException;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static br.com.foody.tracking.modules.order.OrderFixtures.APP_ZONE;
import static br.com.foody.tracking.modules.order.OrderFixtures.NOW_INSTANT;
import static br.com.foody.tracking.modules.order.OrderFixtures.newOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OrderServiceTest {

    private static final OffsetDateTime CREATED_AT =
            OffsetDateTime.ofInstant(NOW_INSTANT.minusSeconds(3600), APP_ZONE);

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
                    Clock.fixed(NOW_INSTANT, APP_ZONE));

    private final User owner =
            new User("Ana Souza", "ana@exemplo.com", "hash", Role.USER, CREATED_AT);
    private final AuthenticatedUser ownerActor =
            new AuthenticatedUser(owner.getPublicId(), Role.USER);
    private final AuthenticatedUser adminActor =
            new AuthenticatedUser(UUID.randomUUID(), Role.ADMIN);

    @Test
    void shouldCreateOrderInRecebidoWithComputedTotal_whenRequestIsValid() {
        // Given
        when(userRepository.findByPublicId(owner.getPublicId())).thenReturn(Optional.of(owner));
        when(orderRepository.nextCode()).thenReturn(1042L);
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        OrderResponseDTO response = service.create(createRequest(), ownerActor);

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
        assertThat(response.createdAt().toInstant()).isEqualTo(NOW_INSTANT);
        assertThat(response.updatedAt().toInstant()).isEqualTo(NOW_INSTANT);
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
        Order order = newOrder(owner, 1042L, CREATED_AT);
        when(orderRepository.findByPublicIdAndOwnerPublicId(
                        order.getPublicId(), owner.getPublicId()))
                .thenReturn(Optional.of(order));

        // When
        OrderResponseDTO response =
                service.changeStatus(
                        order.getPublicId(),
                        new UpdateStatusRequestDTO(OrderStatus.EM_PREPARO),
                        ownerActor);

        // Then
        assertThat(response.status()).isEqualTo(OrderStatus.EM_PREPARO);
        assertThat(response.updatedAt().toInstant()).isEqualTo(NOW_INSTANT);
        verify(orderRepository, never()).findByPublicId(any());
    }

    @Test
    void shouldUseUnscopedQueryAndRecordAdminAsChanger_whenAdminChangesStatus() {
        // Given
        Order order = newOrder(owner, 1042L, CREATED_AT);
        when(orderRepository.findByPublicId(order.getPublicId())).thenReturn(Optional.of(order));

        // When
        service.changeStatus(
                order.getPublicId(), new UpdateStatusRequestDTO(OrderStatus.CANCELADO), adminActor);

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
                                        publicId,
                                        new UpdateStatusRequestDTO(OrderStatus.EM_PREPARO),
                                        ownerActor))
                .isInstanceOf(OrderNotFoundException.class);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void shouldRejectAndPublishNothing_whenTransitionIsNotAllowed() {
        // Given
        Order order = newOrder(owner, 1042L, CREATED_AT);
        when(orderRepository.findByPublicIdAndOwnerPublicId(
                        order.getPublicId(), owner.getPublicId()))
                .thenReturn(Optional.of(order));

        // When / Then
        assertThatThrownBy(
                        () ->
                                service.changeStatus(
                                        order.getPublicId(),
                                        new UpdateStatusRequestDTO(OrderStatus.ENTREGUE),
                                        ownerActor))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessage("Transição de RECEBIDO para ENTREGUE não é permitida");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.RECEBIDO);
        verify(orderRepository, never()).flush();
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void shouldReject_whenTargetIsTheCurrentStatus() {
        // Given
        Order order = newOrder(owner, 1042L, CREATED_AT);
        when(orderRepository.findByPublicIdAndOwnerPublicId(
                        order.getPublicId(), owner.getPublicId()))
                .thenReturn(Optional.of(order));

        // When / Then
        assertThatThrownBy(
                        () ->
                                service.changeStatus(
                                        order.getPublicId(),
                                        new UpdateStatusRequestDTO(OrderStatus.RECEBIDO),
                                        ownerActor))
                .isInstanceOf(InvalidStatusTransitionException.class);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void shouldPublishStatusChangedEvent_whenTransitionSucceeds() {
        // Given
        Order order = newOrder(owner, 1042L, CREATED_AT);
        when(orderRepository.findByPublicIdAndOwnerPublicId(
                        order.getPublicId(), owner.getPublicId()))
                .thenReturn(Optional.of(order));

        // When
        service.changeStatus(
                order.getPublicId(),
                new UpdateStatusRequestDTO(OrderStatus.EM_PREPARO),
                ownerActor);

        // Then
        ArgumentCaptor<OrderStatusChangedEvent> event =
                ArgumentCaptor.forClass(OrderStatusChangedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().orderId()).isEqualTo(order.getPublicId());
        assertThat(event.getValue().code()).isEqualTo(1042L);
        assertThat(event.getValue().fromStatus()).isEqualTo(OrderStatus.RECEBIDO);
        assertThat(event.getValue().toStatus()).isEqualTo(OrderStatus.EM_PREPARO);
        assertThat(event.getValue().changedBy()).isEqualTo(owner.getPublicId());
        assertThat(event.getValue().changedAt().toInstant()).isEqualTo(NOW_INSTANT);
    }

    @Test
    void shouldPropagateAndPublishNothing_whenFlushHitsAStaleVersion() {
        // Given
        Order order = newOrder(owner, 1042L, CREATED_AT);
        when(orderRepository.findByPublicIdAndOwnerPublicId(
                        order.getPublicId(), owner.getPublicId()))
                .thenReturn(Optional.of(order));
        doThrow(new ObjectOptimisticLockingFailureException(Order.class, order.getPublicId()))
                .when(orderRepository)
                .flush();

        // When / Then
        assertThatThrownBy(
                        () ->
                                service.changeStatus(
                                        order.getPublicId(),
                                        new UpdateStatusRequestDTO(OrderStatus.EM_PREPARO),
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
        CreateOrderRequestDTO request =
                new CreateOrderRequestDTO(
                        new CustomerRequestDTO("José da Silva", "", " "),
                        List.of(
                                new OrderItemRequestDTO(
                                        "Pizza calabresa G", 1, new BigDecimal("59.90"))),
                        new AddressRequestDTO(
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
        OrderResponseDTO response = service.create(request, ownerActor);

        // Then
        assertThat(response.notes()).isNull();
        assertThat(response.customer().phone()).isNull();
        assertThat(response.customer().email()).isNull();
        assertThat(response.deliveryAddress().complement()).isNull();
    }

    private static CreateOrderRequestDTO createRequest() {
        return new CreateOrderRequestDTO(
                new CustomerRequestDTO("José da Silva", "+5511987521144", "jose@exemplo.com"),
                List.of(
                        new OrderItemRequestDTO("Pizza calabresa G", 2, new BigDecimal("59.90")),
                        new OrderItemRequestDTO("Refrigerante", 1, new BigDecimal("8.50"))),
                new AddressRequestDTO(
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
