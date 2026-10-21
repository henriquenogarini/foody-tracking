package br.com.foody.tracking.order.application.events;

import br.com.foody.tracking.order.domain.OrderStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderStatusChangedEvent(
    UUID orderId,
    long code,
    OrderStatus fromStatus,
    OrderStatus toStatus,
    UUID changedBy,
    OffsetDateTime changedAt) {}
