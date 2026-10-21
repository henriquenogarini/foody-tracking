package br.com.foody.tracking.modules.order.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.foody.tracking.modules.auth.entity.User;
import br.com.foody.tracking.modules.auth.enums.Role;
import br.com.foody.tracking.modules.order.dto.AddressRequestDTO;
import br.com.foody.tracking.modules.order.dto.CustomerRequestDTO;
import br.com.foody.tracking.modules.order.dto.OrderItemRequestDTO;
import br.com.foody.tracking.modules.order.dto.OrderItemResponseDTO;
import br.com.foody.tracking.modules.order.dto.OrderResponseDTO;
import br.com.foody.tracking.modules.order.dto.OrderSummaryResponseDTO;
import br.com.foody.tracking.modules.order.dto.StatusHistoryResponseDTO;
import br.com.foody.tracking.modules.order.entity.Customer;
import br.com.foody.tracking.modules.order.entity.DeliveryAddress;
import br.com.foody.tracking.modules.order.entity.Order;
import br.com.foody.tracking.modules.order.entity.OrderItem;
import br.com.foody.tracking.modules.order.enums.OrderStatus;
import br.com.foody.tracking.modules.order.repository.OrderSummaryView;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class OrderMapperTest {

  private static final ZoneId APP_ZONE = ZoneId.of("America/Sao_Paulo");
  private static final OffsetDateTime NOW =
      OffsetDateTime.ofInstant(Instant.parse("2026-10-21T17:13:27Z"), APP_ZONE);
  private static final OffsetDateTime LATER = NOW.plusMinutes(10);

  private final OrderMapper mapper = Mappers.getMapper(OrderMapper.class);
  private final User owner = new User("Ana Souza", "ana@exemplo.com", "hash", Role.USER, NOW);

  private Order newOrder() {
    return Order.create(
        owner,
        new Customer("José da Silva", "+5511987521144", "jose@exemplo.com"),
        List.of(
            new OrderItem("Pizza calabresa G", 2, new BigDecimal("59.90")),
            new OrderItem("Refrigerante", 1, new BigDecimal("8.5"))),
        new DeliveryAddress(
            "Rua José Neves",
            "590",
            "sala 03",
            "Vila São Paulo",
            "São Paulo",
            "SP",
            "04650-140",
            -23.6578,
            -46.6743),
        "sem cebola",
        1042L,
        NOW);
  }

  @Test
  void shouldMapFullOrder_whenConvertingToResponse() {
    // Given
    Order order = newOrder();

    // When
    OrderResponseDTO response = mapper.toResponse(order);

    // Then
    assertThat(response.id()).isEqualTo(order.getPublicId());
    assertThat(response.code()).isEqualTo(1042L);
    assertThat(response.status()).isEqualTo(OrderStatus.RECEBIDO);
    assertThat(response.customer().name()).isEqualTo("José da Silva");
    assertThat(response.customer().phone()).isEqualTo("+5511987521144");
    assertThat(response.customer().email()).isEqualTo("jose@exemplo.com");
    assertThat(response.items())
        .extracting(
            OrderItemResponseDTO::description,
            OrderItemResponseDTO::quantity,
            item -> item.unitPrice().toPlainString(),
            item -> item.subtotal().toPlainString())
        .containsExactly(
            tuple("Pizza calabresa G", 2, "59.90", "119.80"),
            tuple("Refrigerante", 1, "8.50", "8.50"));
    assertThat(response.deliveryAddress().street()).isEqualTo("Rua José Neves");
    assertThat(response.deliveryAddress().number()).isEqualTo("590");
    assertThat(response.deliveryAddress().complement()).isEqualTo("sala 03");
    assertThat(response.deliveryAddress().neighborhood()).isEqualTo("Vila São Paulo");
    assertThat(response.deliveryAddress().city()).isEqualTo("São Paulo");
    assertThat(response.deliveryAddress().state()).isEqualTo("SP");
    assertThat(response.deliveryAddress().postalCode()).isEqualTo("04650-140");
    assertThat(response.deliveryAddress().lat()).isEqualTo(-23.6578);
    assertThat(response.deliveryAddress().lng()).isEqualTo(-46.6743);
    assertThat(response.notes()).isEqualTo("sem cebola");
    assertThat(response.total().toPlainString()).isEqualTo("128.30");
    assertThat(response.createdAt()).isAtSameInstantAs(NOW);
    assertThat(response.updatedAt()).isAtSameInstantAs(NOW);
  }

  @Test
  void shouldDeriveSummaryFieldsAndRescaleTotal_whenProjectionDropsTrailingZero() {
    // Given: SQLite returns 59.9 for a stored 59.90
    UUID publicId = UUID.randomUUID();
    OrderSummaryView view = mock(OrderSummaryView.class);
    when(view.getPublicId()).thenReturn(publicId);
    when(view.getCode()).thenReturn(1042L);
    when(view.getStatus()).thenReturn(OrderStatus.EM_PREPARO);
    when(view.getCustomerName()).thenReturn("José da Silva");
    when(view.getTotal()).thenReturn(new BigDecimal("59.9"));
    when(view.getItemsCount()).thenReturn(3);
    when(view.getNeighborhood()).thenReturn("Moema");
    when(view.getCity()).thenReturn("São Paulo");
    when(view.getCreatedAt()).thenReturn(NOW);
    when(view.getUpdatedAt()).thenReturn(LATER);

    // When
    OrderSummaryResponseDTO summary = mapper.toSummary(view);

    // Then
    assertThat(summary.id()).isEqualTo(publicId);
    assertThat(summary.code()).isEqualTo(1042L);
    assertThat(summary.status()).isEqualTo(OrderStatus.EM_PREPARO);
    assertThat(summary.customerName()).isEqualTo("José da Silva");
    assertThat(summary.total().toPlainString()).isEqualTo("59.90");
    assertThat(summary.itemsCount()).isEqualTo(3);
    assertThat(summary.neighborhood()).isEqualTo("Moema");
    assertThat(summary.city()).isEqualTo("São Paulo");
    assertThat(summary.createdAt()).isAtSameInstantAs(NOW);
    assertThat(summary.updatedAt()).isAtSameInstantAs(LATER);
  }

  @Test
  void shouldKeepNullTotal_whenProjectionHasNoTotal() {
    // When / Then
    assertThat(mapper.money(null)).isNull();
  }

  @Test
  void shouldMapHistoryInOrder_whenOrderHasTransitions() {
    // Given
    Order order = newOrder();
    UUID actor = UUID.randomUUID();
    order.changeStatus(OrderStatus.EM_PREPARO, actor, LATER);

    // When
    List<StatusHistoryResponseDTO> history =
        mapper.toHistory(order.getStatusHistory(), Map.of(owner.getPublicId(), "Ana Souza"));

    // Then
    assertThat(history)
        .extracting(
            StatusHistoryResponseDTO::fromStatus,
            StatusHistoryResponseDTO::toStatus,
            StatusHistoryResponseDTO::changedBy,
            StatusHistoryResponseDTO::changedByName)
        .containsExactly(
            tuple(null, OrderStatus.RECEBIDO, owner.getPublicId(), "Ana Souza"),
            // An actor with no matching user (changedBy is not a foreign key) has no name.
            tuple(OrderStatus.RECEBIDO, OrderStatus.EM_PREPARO, actor, null));
    assertThat(history.get(0).changedAt()).isAtSameInstantAs(NOW);
    assertThat(history.get(1).changedAt()).isAtSameInstantAs(LATER);
  }

  @Test
  void shouldBuildDomainValues_whenConvertingRequests() {
    // Given
    CustomerRequestDTO customerRequest = new CustomerRequestDTO("José da Silva", null, null);
    AddressRequestDTO addressRequest =
        new AddressRequestDTO(
            "Rua José Neves",
            "590",
            null,
            "Vila São Paulo",
            "São Paulo",
            "SP",
            "04650140",
            null,
            null);
    OrderItemRequestDTO itemRequest = new OrderItemRequestDTO("Pizza", 3, new BigDecimal("10.5"));

    // When
    Customer customer = mapper.toCustomer(customerRequest);
    DeliveryAddress address = mapper.toAddress(addressRequest);
    OrderItem item = mapper.toItem(itemRequest);

    // Then
    assertThat(customer.getName()).isEqualTo("José da Silva");
    assertThat(customer.getPhone()).isNull();
    assertThat(customer.getEmail()).isNull();
    assertThat(address.getStreet()).isEqualTo("Rua José Neves");
    assertThat(address.getNumber()).isEqualTo("590");
    assertThat(address.getComplement()).isNull();
    assertThat(address.getNeighborhood()).isEqualTo("Vila São Paulo");
    assertThat(address.getCity()).isEqualTo("São Paulo");
    assertThat(address.getState()).isEqualTo("SP");
    assertThat(address.getPostalCode()).isEqualTo("04650140");
    assertThat(address.getLat()).isNull();
    assertThat(address.getLng()).isNull();
    assertThat(item.getDescription()).isEqualTo("Pizza");
    assertThat(item.getQuantity()).isEqualTo(3);
    assertThat(item.getUnitPrice().toPlainString()).isEqualTo("10.50");
    assertThat(item.getSubtotal().toPlainString()).isEqualTo("31.50");
  }

  @Test
  void shouldMapBlankOptionalFieldsToNull_whenRequestsCarryBlankStrings() {
    // Given
    CustomerRequestDTO customerRequest = new CustomerRequestDTO("José da Silva", "", "  ");
    AddressRequestDTO addressRequest =
        new AddressRequestDTO(
            "Rua José Neves",
            "590",
            " ",
            "Vila São Paulo",
            "São Paulo",
            "SP",
            "04650-140",
            null,
            null);

    // When
    Customer customer = mapper.toCustomer(customerRequest);
    DeliveryAddress address = mapper.toAddress(addressRequest);

    // Then
    assertThat(customer.getPhone()).isNull();
    assertThat(customer.getEmail()).isNull();
    assertThat(address.getComplement()).isNull();
  }

  @Test
  void shouldKeepNonBlankOptionalFieldsVerbatim_whenRequestsCarryValues() {
    // Given
    CustomerRequestDTO customerRequest =
        new CustomerRequestDTO("José da Silva", "+5511987521144", "jose@exemplo.com");
    AddressRequestDTO addressRequest =
        new AddressRequestDTO(
            "Rua José Neves",
            "590",
            " sala 03 ",
            "Vila São Paulo",
            "São Paulo",
            "SP",
            "04650-140",
            null,
            null);

    // When
    Customer customer = mapper.toCustomer(customerRequest);
    DeliveryAddress address = mapper.toAddress(addressRequest);

    // Then
    assertThat(customer.getPhone()).isEqualTo("+5511987521144");
    assertThat(customer.getEmail()).isEqualTo("jose@exemplo.com");
    assertThat(address.getComplement()).isEqualTo(" sala 03 ");
  }

  @Test
  void shouldTurnBlankIntoNullAndKeepOtherValues_whenNormalizingOptionalText() {
    // When / Then
    assertThat(mapper.blankToNull(null)).isNull();
    assertThat(mapper.blankToNull("")).isNull();
    assertThat(mapper.blankToNull(" 	 ")).isNull();
    assertThat(mapper.blankToNull("sem cebola")).isEqualTo("sem cebola");
  }

  @Test
  void shouldReturnNull_whenSourcesAreNull() {
    // When / Then
    assertThat(mapper.toResponse(null)).isNull();
    assertThat(mapper.toSummary(null)).isNull();
    assertThat(mapper.toHistory(null, Map.of())).isNull();
    assertThat(mapper.toCustomer(null)).isNull();
    assertThat(mapper.toAddress(null)).isNull();
    assertThat(mapper.toItem(null)).isNull();
  }
}
