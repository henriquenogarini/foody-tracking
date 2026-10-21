package br.com.foody.tracking.modules.order.entity;

import br.com.foody.tracking.modules.order.enums.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Append-only audit row of one status change, including the birth in {@code RECEBIDO} (the only row
 * with a null {@code fromStatus}). Created exclusively by {@link Order}; {@code changedBy} is the
 * actor's public id, never an internal key.
 */
@Entity
@Table(name = "order_status_history")
public class OrderStatusHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", nullable = false, updatable = false)
  private Order order;

  @Enumerated(EnumType.STRING)
  @Column(name = "from_status", updatable = false)
  private OrderStatus fromStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "to_status", nullable = false, updatable = false)
  private OrderStatus toStatus;

  @Column(name = "changed_at", nullable = false, updatable = false)
  private OffsetDateTime changedAt;

  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(name = "changed_by", nullable = false, updatable = false)
  private UUID changedBy;

  protected OrderStatusHistory() {}

  OrderStatusHistory(
      Order order,
      OrderStatus fromStatus,
      OrderStatus toStatus,
      UUID changedBy,
      OffsetDateTime changedAt) {
    this.order = Objects.requireNonNull(order, "order");
    this.fromStatus = fromStatus;
    this.toStatus = Objects.requireNonNull(toStatus, "toStatus");
    this.changedBy = Objects.requireNonNull(changedBy, "changedBy");
    this.changedAt = Objects.requireNonNull(changedAt, "changedAt");
  }

  Order getOrder() {
    return order;
  }

  public Long getId() {
    return id;
  }

  public OrderStatus getFromStatus() {
    return fromStatus;
  }

  public OrderStatus getToStatus() {
    return toStatus;
  }

  public OffsetDateTime getChangedAt() {
    return changedAt;
  }

  public UUID getChangedBy() {
    return changedBy;
  }
}
