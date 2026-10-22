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

public interface OrderRepository extends JpaRepository<Order, Long> {

  String SUMMARY_SELECT =
      "select o.publicId as publicId, o.code as code, o.status as status,"
          + " o.customer.name as customerName, o.total as total,"
          + " size(o.items) as itemsCount,"
          + " o.deliveryAddress.neighborhood as neighborhood, o.deliveryAddress.city as city,"
          + " o.createdAt as createdAt, o.updatedAt as updatedAt"
          + " from Order o";

  String SUMMARY_COUNT = "select count(o) from Order o";

  String SUMMARY_FILTERS =
      " (:status is null or o.status = :status)"
          + " and (:code is null or o.code = :code)"
          + " and (:namePattern is null or lower(o.customer.name) like :namePattern escape '!')";

  Optional<Order> findByPublicIdAndOwnerPublicId(UUID publicId, UUID ownerPublicId);

  Optional<Order> findByPublicId(UUID publicId);

  boolean existsByOwnerPublicId(UUID ownerPublicId);

  @Query(
      value = SUMMARY_SELECT + " where o.owner.publicId = :ownerPublicId and" + SUMMARY_FILTERS,
      countQuery = SUMMARY_COUNT + " where o.owner.publicId = :ownerPublicId and" + SUMMARY_FILTERS)
  Page<OrderSummaryView> searchSummariesByOwnerPublicId(
      @Param("ownerPublicId") UUID ownerPublicId,
      @Param("status") OrderStatus status,
      @Param("code") Long code,
      @Param("namePattern") String namePattern,
      Pageable pageable);

  @Query(
      value = SUMMARY_SELECT + " where" + SUMMARY_FILTERS,
      countQuery = SUMMARY_COUNT + " where" + SUMMARY_FILTERS)
  Page<OrderSummaryView> searchSummaries(
      @Param("status") OrderStatus status,
      @Param("code") Long code,
      @Param("namePattern") String namePattern,
      Pageable pageable);

  /** Safe only in a write transaction: the one-connection pool serializes writers. */
  @Query("select coalesce(max(o.code), 1000) + 1 from Order o")
  long nextCode();
}
