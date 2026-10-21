package br.com.foody.tracking.modules.order.event;

import br.com.foody.tracking.modules.order.enums.OrderStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderStatusChangedEvent(
    UUID orderId,
    long code,
    OrderStatus fromStatus,
    OrderStatus toStatus,
    UUID changedBy,
    OffsetDateTime changedAt) {}
