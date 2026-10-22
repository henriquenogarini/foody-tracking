package br.com.foody.tracking.modules.order.mapper;

import br.com.foody.tracking.modules.order.dto.AddressRequestDTO;
import br.com.foody.tracking.modules.order.dto.AddressResponseDTO;
import br.com.foody.tracking.modules.order.dto.CustomerRequestDTO;
import br.com.foody.tracking.modules.order.dto.CustomerResponseDTO;
import br.com.foody.tracking.modules.order.dto.OrderItemRequestDTO;
import br.com.foody.tracking.modules.order.dto.OrderItemResponseDTO;
import br.com.foody.tracking.modules.order.dto.OrderResponseDTO;
import br.com.foody.tracking.modules.order.dto.OrderSummaryResponseDTO;
import br.com.foody.tracking.modules.order.dto.StatusHistoryResponseDTO;
import br.com.foody.tracking.modules.order.entity.Customer;
import br.com.foody.tracking.modules.order.entity.DeliveryAddress;
import br.com.foody.tracking.modules.order.entity.Money;
import br.com.foody.tracking.modules.order.entity.Order;
import br.com.foody.tracking.modules.order.entity.OrderItem;
import br.com.foody.tracking.modules.order.entity.OrderStatusHistory;
import br.com.foody.tracking.modules.order.repository.OrderSummaryView;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface OrderMapper {

  @Mapping(target = "id", source = "publicId")
  OrderResponseDTO toResponse(Order order);

  CustomerResponseDTO toCustomerResponse(Customer customer);

  AddressResponseDTO toAddressResponse(DeliveryAddress address);

  OrderItemResponseDTO toItemResponse(OrderItem item);

  @Mapping(target = "id", source = "publicId")
  @Mapping(target = "total", source = "total", qualifiedByName = "money")
  OrderSummaryResponseDTO toSummaryResponse(OrderSummaryView view);

  // actorNames is resolved by the service in one query; mapping never touches the database.
  List<StatusHistoryResponseDTO> toHistoryResponses(
      List<OrderStatusHistory> history, @Context Map<UUID, String> actorNames);

  @Mapping(target = "changedByName", source = "changedBy", qualifiedByName = "actorName")
  StatusHistoryResponseDTO toHistoryResponse(
      OrderStatusHistory entry, @Context Map<UUID, String> actorNames);

  // changedBy is not a foreign key: an id without a user maps to null.
  @Named("actorName")
  default String actorName(UUID changedBy, @Context Map<UUID, String> actorNames) {
    return actorNames.get(changedBy);
  }

  @Mapping(target = "phone", source = "phone", qualifiedByName = "blankToNull")
  @Mapping(target = "email", source = "email", qualifiedByName = "blankToNull")
  Customer toCustomer(CustomerRequestDTO request);

  @Mapping(target = "complement", source = "complement", qualifiedByName = "blankToNull")
  DeliveryAddress toAddress(AddressRequestDTO request);

  OrderItem toItem(OrderItemRequestDTO request);

  @Named("blankToNull")
  default String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  // The projection reads total straight from SQLite, which drops trailing zeros (59.9).
  @Named("money")
  default BigDecimal money(BigDecimal value) {
    return value == null ? null : Money.scaled(value);
  }
}
