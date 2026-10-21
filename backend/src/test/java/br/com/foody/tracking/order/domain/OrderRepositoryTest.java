package br.com.foody.tracking.order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import br.com.foody.tracking.auth.domain.Role;
import br.com.foody.tracking.auth.domain.User;
import br.com.foody.tracking.auth.domain.UserRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = Replace.NONE)
class OrderRepositoryTest {

  private static final ZoneId APP_ZONE = ZoneId.of("America/Sao_Paulo");
  private static final OffsetDateTime NOW =
      OffsetDateTime.ofInstant(Instant.parse("2026-10-21T17:13:27Z"), APP_ZONE);

  @Autowired private OrderRepository orderRepository;

  @Autowired private UserRepository userRepository;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private EntityManager entityManager;

  private User ana;

  private User bruno;

  @BeforeEach
  void setUp() {
    ana =
        userRepository.saveAndFlush(
            new User("Ana Souza", "ana@exemplo.com", "hash", Role.USER, NOW));
    bruno =
        userRepository.saveAndFlush(
            new User("Bruno Lima", "bruno@exemplo.com", "hash", Role.USER, NOW));
  }

  private static DeliveryAddress address(String city) {
    return new DeliveryAddress(
        "Rua José Neves",
        "590",
        "sala 03",
        "Vila São Paulo",
        city,
        "SP",
        "04650-140",
        -23.6578,
        -46.6743);
  }

  private Order persistOrder(User owner, long code, OffsetDateTime createdAt, int itemCount) {
    List<OrderItem> items = new ArrayList<>();
    for (int i = 1; i <= itemCount; i++) {
      items.add(new OrderItem("Item " + i, i, new BigDecimal("10.00")));
    }
    Order order =
        Order.create(
            owner,
            new Customer("Cliente " + code, null, null),
            items,
            address("Cidade " + code),
            null,
            code,
            createdAt);
    return orderRepository.saveAndFlush(order);
  }

  private void insertOrderRow(long code, String latLngSql) {
    insertOrderRow(UUID.randomUUID().toString(), code, ana.getId(), latLngSql, "RECEBIDO");
  }

  private void insertOrderRow(
      String publicId, long code, long ownerId, String latLngSql, String status) {
    jdbcTemplate.update(
        "INSERT INTO orders (public_id, code, owner_id, customer_name, address_street,"
            + " address_number, address_neighborhood, address_city, address_state,"
            + " address_postal_code, address_lat, address_lng, status, total, created_at,"
            + " updated_at) VALUES (?, ?, ?, 'Cliente', 'Rua', '1', 'Bairro', 'Cidade', 'SP',"
            + " '04650-140', "
            + latLngSql
            + ", ?, 10.00, ?, ?)",
        publicId,
        code,
        ownerId,
        status,
        Timestamp.from(NOW.toInstant()),
        Timestamp.from(NOW.toInstant()));
  }

  private void insertHistoryRow(long orderId, String fromStatus, String toStatus) {
    jdbcTemplate.update(
        "INSERT INTO order_status_history (order_id, from_status, to_status, changed_at,"
            + " changed_by) VALUES (?, ?, ?, ?, ?)",
        orderId,
        fromStatus,
        toStatus,
        Timestamp.from(NOW.toInstant()),
        UUID.randomUUID().toString());
  }

  @Test
  void shouldRoundTripItemsEmbeddablesAndHistory_whenOrderIsSavedAndReloaded() {
    // Given
    Order order =
        Order.create(
            ana,
            new Customer("José da Silva", "+5511987521144", "jose@exemplo.com"),
            List.of(
                new OrderItem("Pizza calabresa G", 1, new BigDecimal("59.90")),
                new OrderItem("Refrigerante", 2, new BigDecimal("7.50"))),
            address("São Paulo"),
            "sem cebola",
            1001L,
            NOW);
    Order saved = orderRepository.saveAndFlush(order);
    entityManager.clear();

    // When
    Order reloaded = orderRepository.findByPublicId(saved.getPublicId()).orElseThrow();

    // Then
    assertThat(reloaded.getId()).isEqualTo(saved.getId());
    assertThat(reloaded.getPublicId()).isEqualTo(order.getPublicId());
    assertThat(reloaded.getCode()).isEqualTo(1001L);
    assertThat(reloaded.getOwner().getPublicId()).isEqualTo(ana.getPublicId());
    assertThat(reloaded.getStatus()).isEqualTo(OrderStatus.RECEBIDO);
    assertThat(reloaded.getNotes()).isEqualTo("sem cebola");
    assertThat(reloaded.getTotal()).isEqualTo(new BigDecimal("74.90"));
    assertThat(reloaded.getVersion()).isZero();
    assertThat(reloaded.getCreatedAt()).isAtSameInstantAs(NOW);
    assertThat(reloaded.getUpdatedAt()).isAtSameInstantAs(NOW);
    assertThat(reloaded.getCustomer().getName()).isEqualTo("José da Silva");
    assertThat(reloaded.getCustomer().getPhone()).isEqualTo("+5511987521144");
    assertThat(reloaded.getCustomer().getEmail()).isEqualTo("jose@exemplo.com");
    DeliveryAddress address = reloaded.getDeliveryAddress();
    assertThat(address.getStreet()).isEqualTo("Rua José Neves");
    assertThat(address.getNumber()).isEqualTo("590");
    assertThat(address.getComplement()).isEqualTo("sala 03");
    assertThat(address.getNeighborhood()).isEqualTo("Vila São Paulo");
    assertThat(address.getCity()).isEqualTo("São Paulo");
    assertThat(address.getState()).isEqualTo("SP");
    assertThat(address.getPostalCode()).isEqualTo("04650-140");
    assertThat(address.getLat()).isEqualTo(-23.6578);
    assertThat(address.getLng()).isEqualTo(-46.6743);
    assertThat(reloaded.getItems())
        .extracting(
            OrderItem::getDescription,
            OrderItem::getQuantity,
            OrderItem::getUnitPrice,
            OrderItem::getSubtotal)
        .containsExactly(
            tuple("Pizza calabresa G", 1, new BigDecimal("59.90"), new BigDecimal("59.90")),
            tuple("Refrigerante", 2, new BigDecimal("7.50"), new BigDecimal("15.00")));
    assertThat(reloaded.getItems()).allSatisfy(item -> assertThat(item.getId()).isNotNull());
    assertThat(reloaded.getStatusHistory())
        .singleElement()
        .satisfies(
            birth -> {
              assertThat(birth.getId()).isNotNull();
              assertThat(birth.getFromStatus()).isNull();
              assertThat(birth.getToStatus()).isEqualTo(OrderStatus.RECEBIDO);
              assertThat(birth.getChangedBy()).isEqualTo(ana.getPublicId());
              assertThat(birth.getChangedAt()).isAtSameInstantAs(NOW);
            });
  }

  @Test
  void shouldKeepTotalsExact_whenOrderIsAtTheMaximumAcceptedValue() {
    // Given: the largest order the API accepts (50 items x 999 x 99999.99)
    List<OrderItem> items = new ArrayList<>();
    for (int i = 1; i <= 50; i++) {
      items.add(new OrderItem("Item " + i, 999, new BigDecimal("99999.99")));
    }
    Order saved =
        orderRepository.saveAndFlush(
            Order.create(
                ana,
                new Customer("José da Silva", null, null),
                items,
                address("São Paulo"),
                null,
                1001L,
                NOW));
    entityManager.clear();

    // When
    Order reloaded = orderRepository.findByPublicId(saved.getPublicId()).orElseThrow();

    // Then: SQLite stores NUMERIC as REAL, so these must survive the double round-trip
    assertThat(reloaded.getTotal()).isEqualTo(new BigDecimal("4994999500.50"));
    assertThat(reloaded.getItems())
        .hasSize(50)
        .extracting(OrderItem::getSubtotal)
        .containsOnly(new BigDecimal("99899990.01"));
  }

  @Test
  void shouldStartAt1001AndThenIncrementMax_whenComputingNextCode() {
    // Given / When / Then
    assertThat(orderRepository.nextCode()).isEqualTo(1001L);

    // Given
    persistOrder(ana, 1001L, NOW, 1);
    persistOrder(bruno, 1007L, NOW, 1);

    // When / Then
    assertThat(orderRepository.nextCode()).isEqualTo(1008L);
  }

  @Test
  void shouldFindOnlyOwnOrder_whenLookupIsScopedToOwner() {
    // Given
    Order order = persistOrder(ana, 1001L, NOW, 1);
    entityManager.clear();

    // When / Then
    assertThat(
            orderRepository.findByPublicIdAndOwnerPublicId(order.getPublicId(), ana.getPublicId()))
        .isPresent();
    assertThat(
            orderRepository.findByPublicIdAndOwnerPublicId(
                order.getPublicId(), bruno.getPublicId()))
        .isEmpty();
    assertThat(orderRepository.findByPublicId(order.getPublicId())).isPresent();
    assertThat(orderRepository.findByPublicId(UUID.randomUUID())).isEmpty();
  }

  @Test
  void shouldScopeSummariesToOwner_whenListingByOwnerOrUnscoped() {
    // Given
    Order own = persistOrder(ana, 1001L, NOW, 3);
    persistOrder(ana, 1002L, NOW.plusMinutes(1), 1);
    persistOrder(bruno, 1003L, NOW.plusMinutes(2), 2);
    entityManager.clear();
    PageRequest page = PageRequest.of(0, 20, Sort.by("code"));

    // When
    Page<OrderSummaryView> anaPage =
        orderRepository.findSummariesByOwnerPublicId(ana.getPublicId(), page);
    Page<OrderSummaryView> strangerPage =
        orderRepository.findSummariesByOwnerPublicId(UUID.randomUUID(), page);
    Page<OrderSummaryView> allPage = orderRepository.findSummaries(page);

    // Then
    assertThat(anaPage.getTotalElements()).isEqualTo(2);
    assertThat(allPage.getTotalElements()).isEqualTo(3);
    assertThat(strangerPage.getTotalElements()).isZero();
    OrderSummaryView first = anaPage.getContent().get(0);
    assertThat(first.getPublicId()).isEqualTo(own.getPublicId());
    assertThat(first.getCode()).isEqualTo(1001L);
    assertThat(first.getStatus()).isEqualTo(OrderStatus.RECEBIDO);
    assertThat(first.getCustomerName()).isEqualTo("Cliente 1001");
    assertThat(first.getCity()).isEqualTo("Cidade 1001");
    assertThat(first.getItemsCount()).isEqualTo(3);
    assertThat(first.getTotal()).isEqualByComparingTo("60.00");
    assertThat(first.getCreatedAt()).isAtSameInstantAs(NOW);
    assertThat(first.getUpdatedAt()).isAtSameInstantAs(NOW);
    assertThat(allPage.getContent())
        .extracting(OrderSummaryView::getItemsCount)
        .containsExactly(3, 1, 2);
  }

  @Test
  void shouldFilterSummariesByStatus_whenStatusIsGiven() {
    // Given
    Order preparing = persistOrder(ana, 1001L, NOW, 1);
    preparing.changeStatus(OrderStatus.EM_PREPARO, ana.getPublicId(), NOW.plusMinutes(5));
    orderRepository.saveAndFlush(preparing);
    persistOrder(ana, 1002L, NOW, 1);
    Order otherPreparing = persistOrder(bruno, 1003L, NOW, 1);
    otherPreparing.changeStatus(OrderStatus.EM_PREPARO, bruno.getPublicId(), NOW.plusMinutes(5));
    orderRepository.saveAndFlush(otherPreparing);
    entityManager.clear();
    PageRequest page = PageRequest.of(0, 20, Sort.by("code"));

    // When
    Page<OrderSummaryView> anaPreparing =
        orderRepository.findSummariesByOwnerPublicIdAndStatus(
            ana.getPublicId(), OrderStatus.EM_PREPARO, page);
    // A full first page forces Spring Data to run the scoped count query too.
    Page<OrderSummaryView> anaPreparingCounted =
        orderRepository.findSummariesByOwnerPublicIdAndStatus(
            ana.getPublicId(), OrderStatus.EM_PREPARO, PageRequest.of(0, 1, Sort.by("code")));
    Page<OrderSummaryView> allPreparing =
        orderRepository.findSummariesByStatus(OrderStatus.EM_PREPARO, page);

    // Then
    assertThat(anaPreparing.getContent())
        .extracting(OrderSummaryView::getCode)
        .containsExactly(1001L);
    assertThat(anaPreparingCounted.getTotalElements()).isEqualTo(1);
    assertThat(anaPreparing.getContent().get(0).getUpdatedAt())
        .isAtSameInstantAs(NOW.plusMinutes(5));
    assertThat(allPreparing.getContent())
        .extracting(OrderSummaryView::getCode)
        .containsExactly(1001L, 1003L);
    assertThat(allPreparing.getTotalElements()).isEqualTo(2);
  }

  @Test
  void shouldSortSummariesByCreatedAtDescending_whenPageableAsksForIt() {
    // Given
    persistOrder(ana, 1001L, NOW.plusHours(1), 1);
    persistOrder(ana, 1002L, NOW, 1);
    persistOrder(ana, 1003L, NOW.plusHours(2), 1);
    entityManager.clear();

    // When
    Page<OrderSummaryView> page =
        orderRepository.findSummariesByOwnerPublicId(
            ana.getPublicId(), PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")));

    // Then
    assertThat(page.getContent())
        .extracting(OrderSummaryView::getCode)
        .containsExactly(1003L, 1001L, 1002L);
  }

  @Test
  void shouldSortUnscopedSummariesByTotal_whenPageableAsksForIt() {
    // Given: totals 60.00, 10.00 and 30.00
    persistOrder(ana, 1001L, NOW, 3);
    persistOrder(ana, 1002L, NOW, 1);
    persistOrder(bruno, 1003L, NOW, 2);
    entityManager.clear();

    // When
    Page<OrderSummaryView> ascending =
        orderRepository.findSummaries(PageRequest.of(0, 20, Sort.by("total")));
    Page<OrderSummaryView> descending =
        orderRepository.findSummaries(PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "total")));

    // Then
    assertThat(ascending.getContent())
        .extracting(OrderSummaryView::getCode)
        .containsExactly(1002L, 1003L, 1001L);
    assertThat(descending.getContent())
        .extracting(OrderSummaryView::getCode)
        .containsExactly(1001L, 1003L, 1002L);
  }

  @Test
  void shouldSortUnscopedSummariesByStatusThenUpdatedAt_whenPageableAsksForThem() {
    // Given: RECEBIDO, EM_PREPARO (changed later) and CANCELADO (changed earlier)
    persistOrder(ana, 1001L, NOW, 1);
    Order preparing = persistOrder(bruno, 1002L, NOW, 1);
    preparing.changeStatus(OrderStatus.EM_PREPARO, bruno.getPublicId(), NOW.plusMinutes(10));
    orderRepository.saveAndFlush(preparing);
    Order cancelled = persistOrder(ana, 1003L, NOW, 1);
    cancelled.changeStatus(OrderStatus.CANCELADO, ana.getPublicId(), NOW.plusMinutes(5));
    orderRepository.saveAndFlush(cancelled);
    entityManager.clear();

    // When
    Page<OrderSummaryView> byStatus =
        orderRepository.findSummaries(PageRequest.of(0, 20, Sort.by("status")));
    Page<OrderSummaryView> byUpdatedAt =
        orderRepository.findSummaries(
            PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "updatedAt")));

    // Then: status sorts by its stored text
    assertThat(byStatus.getContent())
        .extracting(OrderSummaryView::getStatus)
        .containsExactly(OrderStatus.CANCELADO, OrderStatus.EM_PREPARO, OrderStatus.RECEBIDO);
    assertThat(byUpdatedAt.getContent())
        .extracting(OrderSummaryView::getCode)
        .containsExactly(1002L, 1003L, 1001L);
  }

  @Test
  void shouldLoadSummariesWithoutPerOrderQueries_whenOrdersHaveSeveralItems() {
    // Given
    persistOrder(ana, 1001L, NOW, 3);
    persistOrder(ana, 1002L, NOW.plusMinutes(1), 2);
    persistOrder(ana, 1003L, NOW.plusMinutes(2), 4);
    entityManager.clear();
    Statistics statistics =
        entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
    statistics.clear();

    // When
    Page<OrderSummaryView> page =
        orderRepository.findSummaries(PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "code")));

    // Then
    assertThat(page.getContent()).extracting(OrderSummaryView::getItemsCount).containsExactly(4, 2);
    assertThat(page.getTotalElements()).isEqualTo(3);
    // One select for the page plus one count, regardless of how many orders or items.
    assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
  }

  @Test
  void shouldLoadOwnerScopedSummariesInTwoStatements_whenScopingByOwnerPublicId() {
    // Given
    persistOrder(ana, 1001L, NOW, 3);
    persistOrder(ana, 1002L, NOW.plusMinutes(1), 2);
    persistOrder(ana, 1003L, NOW.plusMinutes(2), 4);
    persistOrder(bruno, 1004L, NOW.plusMinutes(3), 1);
    entityManager.clear();
    Statistics statistics =
        entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
    statistics.clear();

    // When
    Page<OrderSummaryView> page =
        orderRepository.findSummariesByOwnerPublicId(
            ana.getPublicId(), PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "code")));

    // Then
    assertThat(page.getContent())
        .extracting(OrderSummaryView::getCode)
        .containsExactly(1003L, 1002L);
    // The count is scoped like the page: Ana's three orders, never Bruno's.
    assertThat(page.getTotalElements()).isEqualTo(3);
    // The owner join is implicit in each statement: still one page select plus one count.
    assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
  }

  @Test
  void shouldPersistHistoryInOrderAndBumpVersion_whenStatusChanges() {
    // Given
    Order order = persistOrder(ana, 1001L, NOW, 1);
    UUID admin = UUID.randomUUID();

    // When
    order.changeStatus(OrderStatus.EM_PREPARO, admin, NOW);
    order.changeStatus(OrderStatus.SAIU_PARA_ENTREGA, ana.getPublicId(), NOW.plusMinutes(30));
    orderRepository.saveAndFlush(order);
    entityManager.clear();
    Order reloaded = orderRepository.findByPublicId(order.getPublicId()).orElseThrow();

    // Then
    assertThat(reloaded.getVersion()).isEqualTo(1L);
    assertThat(reloaded.getStatus()).isEqualTo(OrderStatus.SAIU_PARA_ENTREGA);
    assertThat(reloaded.getUpdatedAt()).isAtSameInstantAs(NOW.plusMinutes(30));
    assertThat(reloaded.getStatusHistory())
        .extracting(OrderStatusHistory::getFromStatus, OrderStatusHistory::getToStatus)
        .containsExactly(
            tuple(null, OrderStatus.RECEBIDO),
            tuple(OrderStatus.RECEBIDO, OrderStatus.EM_PREPARO),
            tuple(OrderStatus.EM_PREPARO, OrderStatus.SAIU_PARA_ENTREGA));
    assertThat(reloaded.getStatusHistory().get(1).getChangedBy()).isEqualTo(admin);
  }

  @Test
  void shouldCascadeToItemsAndHistory_whenOrderRowIsDeleted() {
    // Given
    Order order = persistOrder(ana, 1001L, NOW, 2);
    order.changeStatus(OrderStatus.CANCELADO, ana.getPublicId(), NOW.plusMinutes(1));
    orderRepository.saveAndFlush(order);
    Long orderId = order.getId();
    entityManager.clear();

    // When
    jdbcTemplate.update("DELETE FROM orders WHERE id = ?", orderId);

    // Then
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM order_items WHERE order_id = ?", Integer.class, orderId))
        .isZero();
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM order_status_history WHERE order_id = ?",
                Integer.class,
                orderId))
        .isZero();
  }

  @Test
  void shouldRejectInsert_whenOnlyOneCoordinateIsPresent() {
    // When / Then
    assertThatThrownBy(() -> insertOrderRow(2001L, "-23.6, NULL"))
        .isInstanceOf(DataAccessException.class)
        .hasMessageContaining("ck_orders_lat_lng");
  }

  @Test
  void shouldRejectInsert_whenCodeIsDuplicated() {
    // Given
    insertOrderRow(2001L, "NULL, NULL");

    // When / Then
    assertThatThrownBy(() -> insertOrderRow(2001L, "NULL, NULL"))
        .isInstanceOf(DataAccessException.class)
        .hasMessageContaining("orders.code");
  }

  @Test
  void shouldRejectInsert_whenStatusIsUnknown() {
    // When / Then
    assertThatThrownBy(
            () ->
                insertOrderRow(
                    UUID.randomUUID().toString(), 2001L, ana.getId(), "NULL, NULL", "PERDIDO"))
        .isInstanceOf(DataAccessException.class)
        .hasMessageContaining("CHECK constraint failed: status");
  }

  @Test
  void shouldRejectInsert_whenPublicIdIsDuplicated() {
    // Given
    String publicId = UUID.randomUUID().toString();
    insertOrderRow(publicId, 2001L, ana.getId(), "NULL, NULL", "RECEBIDO");

    // When / Then
    assertThatThrownBy(() -> insertOrderRow(publicId, 2002L, ana.getId(), "NULL, NULL", "RECEBIDO"))
        .isInstanceOf(DataAccessException.class)
        .hasMessageContaining("orders.public_id");
  }

  @Test
  void shouldRejectInsert_whenOwnerDoesNotExist() {
    // When / Then
    assertThatThrownBy(
            () ->
                insertOrderRow(
                    UUID.randomUUID().toString(), 2001L, 999_999L, "NULL, NULL", "RECEBIDO"))
        .isInstanceOf(DataAccessException.class)
        .hasMessageContaining("FOREIGN KEY constraint failed");
  }

  @Test
  void shouldRejectHistoryRow_whenFromAndToAreTheSame() {
    // Given
    Long orderId = persistOrder(ana, 1001L, NOW, 1).getId();

    // When / Then
    assertThatThrownBy(() -> insertHistoryRow(orderId, "EM_PREPARO", "EM_PREPARO"))
        .isInstanceOf(DataAccessException.class)
        .hasMessageContaining("ck_order_status_history_transition");
  }

  @Test
  void shouldRejectHistoryRow_whenBirthRowIsNotReceived() {
    // Given
    Long orderId = persistOrder(ana, 1001L, NOW, 1).getId();

    // When / Then
    assertThatThrownBy(() -> insertHistoryRow(orderId, null, "EM_PREPARO"))
        .isInstanceOf(DataAccessException.class)
        .hasMessageContaining("ck_order_status_history_birth");
  }

  @Test
  void shouldRejectHistoryRow_whenOrderDoesNotExist() {
    // When / Then
    assertThatThrownBy(() -> insertHistoryRow(999_999L, "RECEBIDO", "EM_PREPARO"))
        .isInstanceOf(DataAccessException.class)
        .hasMessageContaining("FOREIGN KEY constraint failed");
  }
}
