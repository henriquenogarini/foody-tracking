package br.com.foody.tracking.modules.order.repository;

import br.com.foody.tracking.modules.order.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface OrderSummaryView {

  UUID getPublicId();

  long getCode();

  OrderStatus getStatus();

  String getCustomerName();

  BigDecimal getTotal();

  int getItemsCount();

  String getNeighborhood();

  String getCity();

  OffsetDateTime getCreatedAt();

  OffsetDateTime getUpdatedAt();
}
