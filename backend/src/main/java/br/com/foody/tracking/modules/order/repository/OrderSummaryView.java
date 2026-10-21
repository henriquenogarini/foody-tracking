package br.com.foody.tracking.modules.order.repository;

import br.com.foody.tracking.modules.order.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Read-only row of the paged order list. A projection instead of the entity keeps the list at one
 * query (plus the count): {@code itemsCount} is computed by the database, so no item is loaded.
 * {@code total} comes straight from SQLite, which drops trailing zeros; callers normalize the
 * scale.
 */
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
