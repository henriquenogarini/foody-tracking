package br.com.foody.tracking.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * One line of an order. The unit price is normalized to scale 2 and the subtotal is computed here,
 * never taken from the client. Getters re-apply scale 2 because SQLite drops trailing zeros.
 */
@Entity
@Table(name = "order_items")
public class OrderItem {

  private static final BigDecimal MIN_UNIT_PRICE = new BigDecimal("0.01");

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", nullable = false, updatable = false)
  private Order order;

  @Column(name = "description", nullable = false)
  private String description;

  @Column(name = "quantity", nullable = false)
  private int quantity;

  @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
  private BigDecimal unitPrice;

  @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
  private BigDecimal subtotal;

  protected OrderItem() {}

  public OrderItem(String description, int quantity, BigDecimal unitPrice) {
    Objects.requireNonNull(unitPrice, "unitPrice");
    BigDecimal normalizedPrice = unitPrice.setScale(2, RoundingMode.HALF_UP);
    if (quantity < 1) {
      throw new IllegalArgumentException("quantity must be at least 1");
    }
    if (normalizedPrice.compareTo(MIN_UNIT_PRICE) < 0) {
      throw new IllegalArgumentException("unitPrice must be at least 0.01");
    }
    this.description = Objects.requireNonNull(description, "description");
    this.quantity = quantity;
    this.unitPrice = normalizedPrice;
    this.subtotal =
        normalizedPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
  }

  void attachTo(Order order) {
    if (this.order != null && this.order != order) {
      throw new IllegalStateException("item already belongs to another order");
    }
    this.order = order;
  }

  Order getOrder() {
    return order;
  }

  public Long getId() {
    return id;
  }

  public String getDescription() {
    return description;
  }

  public int getQuantity() {
    return quantity;
  }

  public BigDecimal getUnitPrice() {
    return unitPrice.setScale(2, RoundingMode.HALF_UP);
  }

  public BigDecimal getSubtotal() {
    return subtotal.setScale(2, RoundingMode.HALF_UP);
  }
}
