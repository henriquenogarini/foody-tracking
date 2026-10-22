package br.com.foody.tracking.modules.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "order_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    private static final BigDecimal MIN_UNIT_PRICE = new BigDecimal("0.01");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    private Order order;

    @Getter
    @Column(name = "description", nullable = false)
    private String description;

    @Getter
    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    public OrderItem(String description, int quantity, BigDecimal unitPrice) {
        Objects.requireNonNull(unitPrice, "unitPrice");
        BigDecimal normalizedPrice = Money.scaled(unitPrice);
        if (quantity < 1) {
            throw new IllegalArgumentException("quantity must be at least 1");
        }
        if (normalizedPrice.compareTo(MIN_UNIT_PRICE) < 0) {
            throw new IllegalArgumentException("unitPrice must be at least 0.01");
        }
        this.description = Objects.requireNonNull(description, "description");
        this.quantity = quantity;
        this.unitPrice = normalizedPrice;
        this.subtotal = Money.scaled(normalizedPrice.multiply(BigDecimal.valueOf(quantity)));
    }

    void attachTo(Order order) {
        if (this.order != null && this.order != order) {
            throw new IllegalStateException("item already belongs to another order");
        }
        this.order = order;
    }

    // Re-scaled: SQLite drops trailing zeros (59.90 -> 59.9).
    public BigDecimal getUnitPrice() {
        return Money.scaled(unitPrice);
    }

    public BigDecimal getSubtotal() {
        return Money.scaled(subtotal);
    }
}
