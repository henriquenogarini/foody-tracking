package br.com.foody.tracking.modules.order.entity;

import br.com.foody.tracking.modules.auth.entity.User;
import br.com.foody.tracking.modules.order.enums.OrderStatus;
import br.com.foody.tracking.shared.exception.type.InvalidStatusTransitionException;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Aggregate root of a delivery order: it owns its items and its status history, and every change
 * goes through {@link #create} or {@link #changeStatus}. The total is always the sum of the item
 * subtotals so a client can never dictate what is charged. The current instant is passed in by the
 * caller (from the injected {@code Clock}) so the domain stays deterministic and testable.
 */
@Entity
@Table(name = "orders")
public class Order {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(name = "public_id", nullable = false, unique = true, updatable = false)
  private UUID publicId;

  @Column(name = "code", nullable = false, unique = true, updatable = false)
  private long code;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id", nullable = false, updatable = false)
  private User owner;

  @Embedded
  @AttributeOverride(name = "name", column = @Column(name = "customer_name", nullable = false))
  @AttributeOverride(name = "phone", column = @Column(name = "customer_phone"))
  @AttributeOverride(name = "email", column = @Column(name = "customer_email"))
  private Customer customer;

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("id ASC")
  private List<OrderItem> items = new ArrayList<>();

  @Embedded
  @AttributeOverride(name = "street", column = @Column(name = "address_street", nullable = false))
  @AttributeOverride(name = "number", column = @Column(name = "address_number", nullable = false))
  @AttributeOverride(name = "complement", column = @Column(name = "address_complement"))
  @AttributeOverride(
      name = "neighborhood",
      column = @Column(name = "address_neighborhood", nullable = false))
  @AttributeOverride(name = "city", column = @Column(name = "address_city", nullable = false))
  @AttributeOverride(name = "state", column = @Column(name = "address_state", nullable = false))
  @AttributeOverride(
      name = "postalCode",
      column = @Column(name = "address_postal_code", nullable = false))
  @AttributeOverride(name = "lat", column = @Column(name = "address_lat"))
  @AttributeOverride(name = "lng", column = @Column(name = "address_lng"))
  private DeliveryAddress deliveryAddress;

  @Column(name = "notes")
  private String notes;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private OrderStatus status;

  @Column(name = "total", nullable = false, precision = 12, scale = 2)
  private BigDecimal total;

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("changedAt ASC, id ASC")
  private List<OrderStatusHistory> statusHistory = new ArrayList<>();

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @Version
  @Column(name = "version", nullable = false)
  private Long version;

  protected Order() {}

  /**
   * Builds a new order born in {@code RECEBIDO}, with the total computed from the items and the
   * birth recorded as the first history row attributed to the owner.
   */
  public static Order create(
      User owner,
      Customer customer,
      List<OrderItem> items,
      DeliveryAddress deliveryAddress,
      String notes,
      long code,
      OffsetDateTime now) {
    if (items == null || items.isEmpty()) {
      throw new IllegalArgumentException("an order needs at least one item");
    }
    Order order = new Order();
    order.publicId = UUID.randomUUID();
    order.code = code;
    order.owner = Objects.requireNonNull(owner, "owner");
    order.customer = Objects.requireNonNull(customer, "customer");
    order.deliveryAddress = Objects.requireNonNull(deliveryAddress, "deliveryAddress");
    order.notes = notes;
    order.status = OrderStatus.RECEBIDO;
    order.createdAt = Objects.requireNonNull(now, "now");
    order.updatedAt = now;
    BigDecimal sum = BigDecimal.ZERO;
    for (OrderItem item : items) {
      item.attachTo(order);
      order.items.add(item);
      sum = sum.add(item.getSubtotal());
    }
    order.total = Money.scaled(sum);
    order.statusHistory.add(
        new OrderStatusHistory(order, null, OrderStatus.RECEBIDO, owner.getPublicId(), now));
    return order;
  }

  /**
   * Moves the order to {@code target} if {@link OrderStatus#canTransitionTo} allows it, recording
   * who did it and when; otherwise leaves the order untouched.
   *
   * @throws InvalidStatusTransitionException when the transition is not allowed
   */
  public void changeStatus(OrderStatus target, UUID changedBy, OffsetDateTime now) {
    Objects.requireNonNull(target, "target");
    if (!status.canTransitionTo(target)) {
      throw new InvalidStatusTransitionException(status, target);
    }
    statusHistory.add(
        new OrderStatusHistory(
            this,
            status,
            target,
            Objects.requireNonNull(changedBy, "changedBy"),
            Objects.requireNonNull(now, "now")));
    status = target;
    updatedAt = now;
  }

  public Long getId() {
    return id;
  }

  public UUID getPublicId() {
    return publicId;
  }

  public long getCode() {
    return code;
  }

  public User getOwner() {
    return owner;
  }

  public Customer getCustomer() {
    return customer;
  }

  public List<OrderItem> getItems() {
    return Collections.unmodifiableList(items);
  }

  public DeliveryAddress getDeliveryAddress() {
    return deliveryAddress;
  }

  public String getNotes() {
    return notes;
  }

  public OrderStatus getStatus() {
    return status;
  }

  public BigDecimal getTotal() {
    return Money.scaled(total);
  }

  public List<OrderStatusHistory> getStatusHistory() {
    return Collections.unmodifiableList(statusHistory);
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public Long getVersion() {
    return version;
  }
}
