package br.com.foody.tracking.modules.order.mapper;

import br.com.foody.tracking.modules.order.dto.AddressRequest;
import br.com.foody.tracking.modules.order.dto.AddressResponse;
import br.com.foody.tracking.modules.order.dto.CustomerRequest;
import br.com.foody.tracking.modules.order.dto.CustomerResponse;
import br.com.foody.tracking.modules.order.dto.OrderItemRequest;
import br.com.foody.tracking.modules.order.dto.OrderItemResponse;
import br.com.foody.tracking.modules.order.dto.OrderResponse;
import br.com.foody.tracking.modules.order.dto.OrderSummaryResponse;
import br.com.foody.tracking.modules.order.dto.StatusHistoryResponse;
import br.com.foody.tracking.modules.order.entity.Customer;
import br.com.foody.tracking.modules.order.entity.DeliveryAddress;
import br.com.foody.tracking.modules.order.entity.Order;
import br.com.foody.tracking.modules.order.entity.OrderItem;
import br.com.foody.tracking.modules.order.entity.OrderStatusHistory;
import br.com.foody.tracking.modules.order.repository.OrderSummaryView;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface OrderMapper {

  @Mapping(target = "id", source = "publicId")
  OrderResponse toResponse(Order order);

  CustomerResponse toCustomerResponse(Customer customer);

  AddressResponse toAddressResponse(DeliveryAddress address);

  OrderItemResponse toItemResponse(OrderItem item);

  @Mapping(target = "id", source = "publicId")
  @Mapping(target = "total", source = "total", qualifiedByName = "money")
  OrderSummaryResponse toSummary(OrderSummaryView view);

  List<StatusHistoryResponse> toHistory(List<OrderStatusHistory> history);

  StatusHistoryResponse toHistoryResponse(OrderStatusHistory entry);

  @Mapping(target = "phone", source = "phone", qualifiedByName = "blankToNull")
  @Mapping(target = "email", source = "email", qualifiedByName = "blankToNull")
  Customer toCustomer(CustomerRequest request);

  @Mapping(target = "complement", source = "complement", qualifiedByName = "blankToNull")
  DeliveryAddress toAddress(AddressRequest request);

  OrderItem toItem(OrderItemRequest request);

  // Optional text sent as "" or "  " means "absent"; non-blank values are kept verbatim.
  @Named("blankToNull")
  default String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  // The projection reads total straight from SQLite, which drops trailing zeros (59.9).
  @Named("money")
  default BigDecimal money(BigDecimal value) {
    return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
  }
}
