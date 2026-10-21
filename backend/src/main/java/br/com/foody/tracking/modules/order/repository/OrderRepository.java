package br.com.foody.tracking.modules.order.repository;

import br.com.foody.tracking.modules.order.entity.Order;
import br.com.foody.tracking.modules.order.enums.OrderStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Orders and their paged summaries. The role scope of the caller is expressed by choosing the
 * owner-scoped or the unscoped query, never by filtering in memory.
 */
public interface OrderRepository extends JpaRepository<Order, Long> {

  String SUMMARY_SELECT =
      "select o.publicId as publicId, o.code as code, o.status as status,"
          + " o.customer.name as customerName, o.total as total,"
          + " size(o.items) as itemsCount,"
          + " o.deliveryAddress.neighborhood as neighborhood, o.deliveryAddress.city as city,"
          + " o.createdAt as createdAt, o.updatedAt as updatedAt"
          + " from Order o";

  /** Owner scope by the JWT {@code sub}, so the caller needs no extra user lookup. */
  Optional<Order> findByPublicIdAndOwnerPublicId(UUID publicId, UUID ownerPublicId);

  Optional<Order> findByPublicId(UUID publicId);

  @Query(
      value = SUMMARY_SELECT + " where o.owner.publicId = :ownerPublicId",
      countQuery = "select count(o) from Order o where o.owner.publicId = :ownerPublicId")
  Page<OrderSummaryView> findSummariesByOwnerPublicId(
      @Param("ownerPublicId") UUID ownerPublicId, Pageable pageable);

  @Query(
      value = SUMMARY_SELECT + " where o.owner.publicId = :ownerPublicId and o.status = :status",
      countQuery =
          "select count(o) from Order o"
              + " where o.owner.publicId = :ownerPublicId and o.status = :status")
  Page<OrderSummaryView> findSummariesByOwnerPublicIdAndStatus(
      @Param("ownerPublicId") UUID ownerPublicId,
      @Param("status") OrderStatus status,
      Pageable pageable);

  @Query(value = SUMMARY_SELECT, countQuery = "select count(o) from Order o")
  Page<OrderSummaryView> findSummaries(Pageable pageable);

  @Query(
      value = SUMMARY_SELECT + " where o.status = :status",
      countQuery = "select count(o) from Order o where o.status = :status")
  Page<OrderSummaryView> findSummariesByStatus(
      @Param("status") OrderStatus status, Pageable pageable);

  /**
   * Next readable order code, starting at 1001. Safe only inside the write transaction: the
   * single-connection pool serializes writers, and {@code uq_orders_code} is the last guard.
   */
  @Query("select coalesce(max(o.code), 1000) + 1 from Order o")
  long nextCode();
}
