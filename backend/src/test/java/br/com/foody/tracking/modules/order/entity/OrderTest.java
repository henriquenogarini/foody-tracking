package br.com.foody.tracking.modules.order.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.foody.tracking.modules.auth.entity.User;
import br.com.foody.tracking.modules.auth.enums.Role;
import br.com.foody.tracking.modules.order.enums.OrderStatus;
import br.com.foody.tracking.shared.exception.type.InvalidStatusTransitionException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class OrderTest {

  private static final ZoneId APP_ZONE = ZoneId.of("America/Sao_Paulo");
  private static final OffsetDateTime NOW =
      OffsetDateTime.ofInstant(Instant.parse("2026-10-21T17:13:27Z"), APP_ZONE);
  private static final OffsetDateTime LATER = NOW.plusMinutes(10);

  private final User owner = new User("Ana Souza", "ana@exemplo.com", "hash", Role.USER, NOW);

  private static Customer customer() {
    return new Customer("José da Silva", "+5511987521144", "jose@exemplo.com");
  }

  private static DeliveryAddress address() {
    return new DeliveryAddress(
        "Rua José Neves",
        "590",
        "sala 03",
        "Vila São Paulo",
        "São Paulo",
        "SP",
        "04650-140",
        -23.6578,
        -46.6743);
  }

  private Order newOrder(List<OrderItem> items) {
    return Order.create(owner, customer(), items, address(), "sem cebola", 1001L, NOW);
  }

  private Order orderIn(OrderStatus status) {
    Order order = newOrder(List.of(new OrderItem("Pizza", 1, new BigDecimal("59.90"))));
    switch (status) {
      case RECEBIDO -> {}
      case EM_PREPARO -> order.changeStatus(OrderStatus.EM_PREPARO, owner.getPublicId(), NOW);
      case SAIU_PARA_ENTREGA -> {
        order.changeStatus(OrderStatus.EM_PREPARO, owner.getPublicId(), NOW);
        order.changeStatus(OrderStatus.SAIU_PARA_ENTREGA, owner.getPublicId(), NOW);
      }
      case ENTREGUE -> {
        order.changeStatus(OrderStatus.EM_PREPARO, owner.getPublicId(), NOW);
        order.changeStatus(OrderStatus.SAIU_PARA_ENTREGA, owner.getPublicId(), NOW);
        order.changeStatus(OrderStatus.ENTREGUE, owner.getPublicId(), NOW);
      }
      case CANCELADO -> order.changeStatus(OrderStatus.CANCELADO, owner.getPublicId(), NOW);
    }
    return order;
  }

  @Test
  void shouldBeBornReceivedWithTotalAndBirthHistory_whenCreated() {
    // Given
    OrderItem pizza = new OrderItem("Pizza calabresa G", 1, new BigDecimal("59.90"));
    OrderItem soda = new OrderItem("Refrigerante", 2, new BigDecimal("10.005"));

    // When
    Order order = newOrder(List.of(pizza, soda));

    // Then
    assertThat(order.getPublicId()).isNotNull();
    assertThat(order.getCode()).isEqualTo(1001L);
    assertThat(order.getOwner()).isSameAs(owner);
    assertThat(order.getStatus()).isEqualTo(OrderStatus.RECEBIDO);
    assertThat(order.getNotes()).isEqualTo("sem cebola");
    assertThat(order.getCustomer().getName()).isEqualTo("José da Silva");
    assertThat(order.getDeliveryAddress().getCity()).isEqualTo("São Paulo");
    assertThat(soda.getUnitPrice()).isEqualTo(new BigDecimal("10.01"));
    assertThat(soda.getSubtotal()).isEqualTo(new BigDecimal("20.02"));
    assertThat(pizza.getSubtotal()).isEqualTo(new BigDecimal("59.90"));
    assertThat(order.getTotal()).isEqualTo(new BigDecimal("79.92"));
    assertThat(order.getCreatedAt()).isEqualTo(NOW);
    assertThat(order.getUpdatedAt()).isEqualTo(NOW);
    assertThat(order.getItems()).containsExactly(pizza, soda);
    assertThat(order.getStatusHistory())
        .singleElement()
        .satisfies(
            birth -> {
              assertThat(birth.getFromStatus()).isNull();
              assertThat(birth.getToStatus()).isEqualTo(OrderStatus.RECEBIDO);
              assertThat(birth.getChangedBy()).isEqualTo(owner.getPublicId());
              assertThat(birth.getChangedAt()).isEqualTo(NOW);
            });
  }

  @Test
  void shouldKeepScaleTwo_whenSingleItemPriceHasTrailingZero() {
    // When
    Order order = newOrder(List.of(new OrderItem("Pizza", 1, new BigDecimal("59.9"))));

    // Then
    assertThat(order.getTotal()).isEqualTo(new BigDecimal("59.90"));
  }

  @Test
  void shouldExposeUnmodifiableCollections_whenOrderIsCreated() {
    // Given
    Order order = newOrder(List.of(new OrderItem("Pizza", 1, new BigDecimal("59.90"))));
    OrderItem extra = new OrderItem("Extra", 1, BigDecimal.ONE);

    // When / Then
    assertThatThrownBy(() -> order.getItems().add(extra))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> order.getStatusHistory().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void shouldReject_whenCreatedWithoutItems() {
    // When / Then
    assertThatThrownBy(() -> newOrder(List.of())).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newOrder(null)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldAppendHistoryAndUpdateStatus_whenTransitionIsAllowed() {
    // Given
    Order order = newOrder(List.of(new OrderItem("Pizza", 1, new BigDecimal("59.90"))));
    UUID actor = UUID.randomUUID();

    // When
    order.changeStatus(OrderStatus.EM_PREPARO, actor, LATER);

    // Then
    assertThat(order.getStatus()).isEqualTo(OrderStatus.EM_PREPARO);
    assertThat(order.getUpdatedAt()).isEqualTo(LATER);
    assertThat(order.getCreatedAt()).isEqualTo(NOW);
    assertThat(order.getStatusHistory()).hasSize(2);
    OrderStatusHistory last = order.getStatusHistory().get(1);
    assertThat(last.getFromStatus()).isEqualTo(OrderStatus.RECEBIDO);
    assertThat(last.getToStatus()).isEqualTo(OrderStatus.EM_PREPARO);
    assertThat(last.getChangedBy()).isEqualTo(actor);
    assertThat(last.getChangedAt()).isEqualTo(LATER);
  }

  @Test
  void shouldThrowAndKeepState_whenTransitionIsForbidden() {
    // Given
    Order order = orderIn(OrderStatus.SAIU_PARA_ENTREGA);
    int historySize = order.getStatusHistory().size();
    OffsetDateTime updatedAt = order.getUpdatedAt();

    // When / Then
    assertThatThrownBy(() -> order.changeStatus(OrderStatus.CANCELADO, UUID.randomUUID(), LATER))
        .isInstanceOf(InvalidStatusTransitionException.class)
        .hasMessage("Transição de SAIU_PARA_ENTREGA para CANCELADO não é permitida");
    assertThat(order.getStatus()).isEqualTo(OrderStatus.SAIU_PARA_ENTREGA);
    assertThat(order.getStatusHistory()).hasSize(historySize);
    assertThat(order.getUpdatedAt()).isEqualTo(updatedAt);
  }

  @ParameterizedTest
  @EnumSource(OrderStatus.class)
  void shouldReject_whenTargetIsTheCurrentStatus(OrderStatus status) {
    // Given
    Order order = orderIn(status);
    int historySize = order.getStatusHistory().size();
    OffsetDateTime updatedAt = order.getUpdatedAt();

    // When / Then
    assertThatThrownBy(() -> order.changeStatus(status, owner.getPublicId(), LATER))
        .isInstanceOf(InvalidStatusTransitionException.class);
    assertThat(order.getStatus()).isEqualTo(status);
    assertThat(order.getStatusHistory()).hasSize(historySize);
    assertThat(order.getUpdatedAt()).isEqualTo(updatedAt);
  }

  @Test
  void shouldRejectReparenting_whenItemAlreadyBelongsToAnotherOrder() {
    // Given
    OrderItem pizza = new OrderItem("Pizza", 1, new BigDecimal("59.90"));
    Order first = newOrder(List.of(pizza));
    List<OrderItem> reused = List.of(pizza);

    // When / Then
    assertThatThrownBy(() -> newOrder(reused))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("item already belongs to another order");
    assertThat(first.getItems()).containsExactly(pizza);
  }

  @ParameterizedTest
  @EnumSource(
      value = OrderStatus.class,
      names = {"ENTREGUE", "CANCELADO"})
  void shouldRejectEveryTarget_whenStatusIsTerminal(OrderStatus terminal) {
    // Given
    Order order = orderIn(terminal);

    // When / Then
    for (OrderStatus target : OrderStatus.values()) {
      assertThatThrownBy(() -> order.changeStatus(target, owner.getPublicId(), LATER))
          .isInstanceOf(InvalidStatusTransitionException.class);
    }
    assertThat(order.getStatus()).isEqualTo(terminal);
  }

  @Test
  void shouldReject_whenOnlyOneCoordinateIsPresent() {
    // When / Then
    assertThatThrownBy(
            () ->
                new DeliveryAddress(
                    "Rua", "1", null, "Bairro", "Cidade", "SP", "04650-140", -23.6, null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                new DeliveryAddress(
                    "Rua", "1", null, "Bairro", "Cidade", "SP", "04650-140", null, -46.6))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldAcceptAddress_whenCoordinatesAreBothAbsent() {
    // When
    DeliveryAddress address =
        new DeliveryAddress("Rua", "1", null, "Bairro", "Cidade", "SP", "04650140", null, null);

    // Then
    assertThat(address.getLat()).isNull();
    assertThat(address.getLng()).isNull();
  }

  @Test
  void shouldRejectItem_whenQuantityOrPriceIsBelowMinimum() {
    // When / Then
    assertThatThrownBy(() -> new OrderItem("Pizza", 0, new BigDecimal("10.00")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new OrderItem("Pizza", 1, new BigDecimal("0.00")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new OrderItem("Pizza", 1, new BigDecimal("0.004")))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
