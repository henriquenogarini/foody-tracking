package br.com.foody.tracking.modules.order.repository;

import br.com.foody.tracking.modules.auth.entity.User;
import br.com.foody.tracking.modules.auth.enums.Role;
import br.com.foody.tracking.modules.auth.repository.UserRepository;
import br.com.foody.tracking.modules.order.entity.Customer;
import br.com.foody.tracking.modules.order.entity.DeliveryAddress;
import br.com.foody.tracking.modules.order.entity.Order;
import br.com.foody.tracking.modules.order.entity.OrderItem;
import br.com.foody.tracking.modules.order.entity.OrderStatusHistory;
import br.com.foody.tracking.modules.order.enums.OrderStatus;
import jakarta.persistence.EntityManager;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static br.com.foody.tracking.modules.order.OrderFixtures.NOW;
import static br.com.foody.tracking.modules.order.OrderFixtures.customer;
import static br.com.foody.tracking.modules.order.OrderFixtures.pizza;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = Replace.NONE)
class OrderRepositoryTest {

    private static final Pageable BY_CODE = PageRequest.of(0, 20, Sort.by("code"));

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

    private Statistics clearedStatistics() {
        Statistics statistics =
                entityManager
                        .getEntityManagerFactory()
                        .unwrap(SessionFactory.class)
                        .getStatistics();
        statistics.clear();
        return statistics;
    }

    private Order persistNamedOrder(User owner, long code, String customerName) {
        return orderRepository.saveAndFlush(
                Order.create(
                        owner,
                        new Customer(customerName, null, null),
                        List.of(new OrderItem("Item", 1, new BigDecimal("10.00"))),
                        address("São Paulo"),
                        null,
                        code,
                        NOW));
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
        insertOrderRow(publicId, code, ownerId, latLngSql, status, "Cliente", "SP", "10.00");
    }

    private void insertOrderRow(String customerName, String state, String total) {
        insertOrderRow(
                UUID.randomUUID().toString(),
                2001L,
                ana.getId(),
                "NULL, NULL",
                "RECEBIDO",
                customerName,
                state,
                total);
    }

    private void insertOrderRow(
            String publicId,
            long code,
            long ownerId,
            String latLngSql,
            String status,
            String customerName,
            String state,
            String total) {
        jdbcTemplate.update(
                "INSERT INTO orders (public_id, code, owner_id, customer_name, address_street,"
                        + " address_number, address_neighborhood, address_city, address_state,"
                        + " address_postal_code, address_lat, address_lng, status, total, created_at,"
                        + " updated_at) VALUES (?, ?, ?, ?, 'Rua', '1', 'Bairro', 'Cidade', ?,"
                        + " '04650-140', "
                        + latLngSql
                        + ", ?, ?, ?, ?)",
                publicId,
                code,
                ownerId,
                customerName,
                state,
                status,
                new BigDecimal(total),
                Timestamp.from(NOW.toInstant()),
                Timestamp.from(NOW.toInstant()));
    }

    private void insertItemRow(
            long orderId, String description, int quantity, String unitPrice, String subtotal) {
        jdbcTemplate.update(
                "INSERT INTO order_items (order_id, description, quantity, unit_price, subtotal)"
                        + " VALUES (?, ?, ?, ?, ?)",
                orderId,
                description,
                quantity,
                new BigDecimal(unitPrice),
                new BigDecimal(subtotal));
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
                        customer(),
                        List.of(pizza(), new OrderItem("Refrigerante", 2, new BigDecimal("7.50"))),
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
                        tuple(
                                "Pizza calabresa G",
                                1,
                                new BigDecimal("59.90"),
                                new BigDecimal("59.90")),
                        tuple("Refrigerante", 2, new BigDecimal("7.50"), new BigDecimal("15.00")));
        assertThat(reloaded.getStatusHistory())
                .singleElement()
                .satisfies(
                        birth -> {
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
                        orderRepository.findByPublicIdAndOwnerPublicId(
                                order.getPublicId(), ana.getPublicId()))
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
                orderRepository.searchSummariesByOwnerPublicId(
                        ana.getPublicId(), null, null, null, page);
        Page<OrderSummaryView> strangerPage =
                orderRepository.searchSummariesByOwnerPublicId(
                        UUID.randomUUID(), null, null, null, page);
        Page<OrderSummaryView> allPage = orderRepository.searchSummaries(null, null, null, page);

        // Then
        assertThat(anaPage.getTotalElements()).isEqualTo(2);
        assertThat(allPage.getTotalElements()).isEqualTo(3);
        assertThat(strangerPage.getTotalElements()).isZero();
        OrderSummaryView first = anaPage.getContent().get(0);
        assertThat(first.getPublicId()).isEqualTo(own.getPublicId());
        assertThat(first.getCode()).isEqualTo(1001L);
        assertThat(first.getStatus()).isEqualTo(OrderStatus.RECEBIDO);
        assertThat(first.getCustomerName()).isEqualTo("Cliente 1001");
        assertThat(first.getNeighborhood()).isEqualTo("Vila São Paulo");
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
        otherPreparing.changeStatus(
                OrderStatus.EM_PREPARO, bruno.getPublicId(), NOW.plusMinutes(5));
        orderRepository.saveAndFlush(otherPreparing);
        entityManager.clear();
        PageRequest page = PageRequest.of(0, 20, Sort.by("code"));

        // When
        Page<OrderSummaryView> anaPreparing =
                orderRepository.searchSummariesByOwnerPublicId(
                        ana.getPublicId(), OrderStatus.EM_PREPARO, null, null, page);
        // A full first page forces Spring Data to run the scoped count query too.
        Page<OrderSummaryView> anaPreparingCounted =
                orderRepository.searchSummariesByOwnerPublicId(
                        ana.getPublicId(),
                        OrderStatus.EM_PREPARO,
                        null,
                        null,
                        PageRequest.of(0, 1, Sort.by("code")));
        Page<OrderSummaryView> allPreparing =
                orderRepository.searchSummaries(OrderStatus.EM_PREPARO, null, null, page);

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
    void shouldPageDeterministicallyByCode_whenCreatedAtTiesAndCodeBreaksThem() {
        // Given: four orders created in the same millisecond, two of them by another owner
        persistOrder(ana, 1001L, NOW, 1);
        persistOrder(bruno, 1002L, NOW, 1);
        persistOrder(ana, 1003L, NOW, 1);
        persistOrder(bruno, 1004L, NOW, 1);
        entityManager.clear();
        Sort stable = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("code"));

        // When
        Page<OrderSummaryView> first =
                orderRepository.searchSummaries(null, null, null, PageRequest.of(0, 2, stable));
        Page<OrderSummaryView> second =
                orderRepository.searchSummaries(null, null, null, PageRequest.of(1, 2, stable));
        Page<OrderSummaryView> anaOnly =
                orderRepository.searchSummariesByOwnerPublicId(
                        ana.getPublicId(), null, null, null, PageRequest.of(0, 20, stable));

        // Then: no row repeats or goes missing across pages
        assertThat(first.getContent())
                .extracting(OrderSummaryView::getCode)
                .containsExactly(1004L, 1003L);
        assertThat(second.getContent())
                .extracting(OrderSummaryView::getCode)
                .containsExactly(1002L, 1001L);
        assertThat(anaOnly.getContent())
                .extracting(OrderSummaryView::getCode)
                .containsExactly(1003L, 1001L);
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
                orderRepository.searchSummariesByOwnerPublicId(
                        ana.getPublicId(),
                        null,
                        null,
                        null,
                        PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")));

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
                orderRepository.searchSummaries(
                        null, null, null, PageRequest.of(0, 20, Sort.by("total")));
        Page<OrderSummaryView> descending =
                orderRepository.searchSummaries(
                        null,
                        null,
                        null,
                        PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "total")));

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
                orderRepository.searchSummaries(
                        null, null, null, PageRequest.of(0, 20, Sort.by("status")));
        Page<OrderSummaryView> byUpdatedAt =
                orderRepository.searchSummaries(
                        null,
                        null,
                        null,
                        PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "updatedAt")));

        // Then: status sorts by its stored text
        assertThat(byStatus.getContent())
                .extracting(OrderSummaryView::getStatus)
                .containsExactly(
                        OrderStatus.CANCELADO, OrderStatus.EM_PREPARO, OrderStatus.RECEBIDO);
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
        Statistics statistics = clearedStatistics();

        // When
        Page<OrderSummaryView> page =
                orderRepository.searchSummaries(
                        null,
                        null,
                        null,
                        PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "code")));

        // Then
        assertThat(page.getContent())
                .extracting(OrderSummaryView::getItemsCount)
                .containsExactly(4, 2);
        assertThat(page.getTotalElements()).isEqualTo(3);
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
        Statistics statistics = clearedStatistics();

        // When
        Page<OrderSummaryView> page =
                orderRepository.searchSummariesByOwnerPublicId(
                        ana.getPublicId(),
                        null,
                        null,
                        null,
                        PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "code")));

        // Then
        assertThat(page.getContent())
                .extracting(OrderSummaryView::getCode)
                .containsExactly(1003L, 1002L);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
    }

    @Test
    void shouldMatchNameContainsIgnoringAsciiCase_whenSearchingByNamePattern() {
        // Given
        persistNamedOrder(ana, 2001L, "José da Silva");
        persistNamedOrder(ana, 2002L, "Maria SILVEIRA");
        persistNamedOrder(ana, 2003L, "Ana Souza");
        entityManager.clear();

        // When
        Page<OrderSummaryView> page =
                orderRepository.searchSummaries(null, null, "%silv%", BY_CODE);

        // Then
        assertThat(page.getContent())
                .extracting(OrderSummaryView::getCode)
                .containsExactly(2001L, 2002L);
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void shouldMatchOnlyThatCode_whenSearchingByCode() {
        // Given
        persistNamedOrder(ana, 2001L, "Cliente 2001");
        persistNamedOrder(ana, 2002L, "Cliente 2001 bis");
        entityManager.clear();

        // When
        Page<OrderSummaryView> page = orderRepository.searchSummaries(null, 2001L, null, BY_CODE);

        // Then
        assertThat(page.getContent()).extracting(OrderSummaryView::getCode).containsExactly(2001L);
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void shouldCombineSearchWithStatusAndOwnerScope_whenUserSearches() {
        // Given
        Order preparing = persistNamedOrder(ana, 2001L, "Ana Souza");
        preparing.changeStatus(OrderStatus.EM_PREPARO, ana.getPublicId(), NOW.plusMinutes(1));
        orderRepository.saveAndFlush(preparing);
        persistNamedOrder(ana, 2002L, "Ana Souza");
        persistNamedOrder(bruno, 2003L, "Ana Souza");
        entityManager.clear();

        // When
        Page<OrderSummaryView> anaPreparing =
                orderRepository.searchSummariesByOwnerPublicId(
                        ana.getPublicId(), OrderStatus.EM_PREPARO, null, "%ana%", BY_CODE);
        Page<OrderSummaryView> anaAll =
                orderRepository.searchSummariesByOwnerPublicId(
                        ana.getPublicId(), null, null, "%ana%", BY_CODE);
        Page<OrderSummaryView> brunoCode =
                orderRepository.searchSummariesByOwnerPublicId(
                        bruno.getPublicId(), null, 2001L, null, BY_CODE);

        // Then
        assertThat(anaPreparing.getContent())
                .extracting(OrderSummaryView::getCode)
                .containsExactly(2001L);
        assertThat(anaAll.getContent())
                .extracting(OrderSummaryView::getCode)
                .containsExactly(2001L, 2002L);
        assertThat(anaAll.getTotalElements()).isEqualTo(2);
        assertThat(brunoCode.getContent()).isEmpty();
        assertThat(brunoCode.getTotalElements()).isZero();
    }

    @Test
    void shouldTreatEscapedWildcardsLiterally_whenNameContainsPercentOrUnderscore() {
        // Given
        persistNamedOrder(ana, 2001L, "Promo 50% off");
        persistNamedOrder(ana, 2002L, "Promo 500 off");
        persistNamedOrder(ana, 2003L, "a_b");
        persistNamedOrder(ana, 2004L, "axb");
        persistNamedOrder(ana, 2005L, "Bang! Lanches");
        entityManager.clear();

        // When
        Page<OrderSummaryView> percent =
                orderRepository.searchSummaries(null, null, "%50!%%", BY_CODE);
        Page<OrderSummaryView> underscore =
                orderRepository.searchSummaries(null, null, "%a!_b%", BY_CODE);
        Page<OrderSummaryView> bang = orderRepository.searchSummaries(null, null, "%g!!%", BY_CODE);

        // Then
        assertThat(percent.getContent())
                .extracting(OrderSummaryView::getCode)
                .containsExactly(2001L);
        assertThat(underscore.getContent())
                .extracting(OrderSummaryView::getCode)
                .containsExactly(2003L);
        assertThat(bang.getContent()).extracting(OrderSummaryView::getCode).containsExactly(2005L);
    }

    @Test
    void shouldStillUseTwoStatements_whenEveryFilterIsActive() {
        // Given
        for (long code = 2001L; code <= 2004L; code++) {
            persistNamedOrder(ana, code, "Ana Souza");
        }
        entityManager.clear();
        Statistics statistics = clearedStatistics();

        // When
        Page<OrderSummaryView> page =
                orderRepository.searchSummariesByOwnerPublicId(
                        ana.getPublicId(),
                        OrderStatus.RECEBIDO,
                        null,
                        "%souza%",
                        PageRequest.of(0, 2, BY_CODE.getSort()));

        // Then
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(4);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
    }

    @Test
    void shouldReportOwnership_whenCheckingIfOwnerHasOrders() {
        // Given
        persistOrder(ana, 1001L, NOW, 1);

        // When / Then
        assertThat(orderRepository.existsByOwnerPublicId(ana.getPublicId())).isTrue();
        assertThat(orderRepository.existsByOwnerPublicId(bruno.getPublicId())).isFalse();
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
                                "SELECT COUNT(*) FROM order_items WHERE order_id = ?",
                                Integer.class,
                                orderId))
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
                                        UUID.randomUUID().toString(),
                                        2001L,
                                        ana.getId(),
                                        "NULL, NULL",
                                        "PERDIDO"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("CHECK constraint failed: status");
    }

    @Test
    void shouldRejectInsert_whenPublicIdIsDuplicated() {
        // Given
        String publicId = UUID.randomUUID().toString();
        insertOrderRow(publicId, 2001L, ana.getId(), "NULL, NULL", "RECEBIDO");

        // When / Then
        assertThatThrownBy(
                        () ->
                                insertOrderRow(
                                        publicId, 2002L, ana.getId(), "NULL, NULL", "RECEBIDO"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("orders.public_id");
    }

    @Test
    void shouldRejectInsert_whenOwnerDoesNotExist() {
        // When / Then
        assertThatThrownBy(
                        () ->
                                insertOrderRow(
                                        UUID.randomUUID().toString(),
                                        2001L,
                                        999_999L,
                                        "NULL, NULL",
                                        "RECEBIDO"))
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

    @Test
    void shouldRejectInsert_whenCustomerNameIsEmpty() {
        // When / Then
        assertThatThrownBy(() -> insertOrderRow("", "SP", "10.00"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("CHECK constraint failed: length(customer_name) > 0");
    }

    @Test
    void shouldRejectInsert_whenStateIsNotTwoLetters() {
        // When / Then
        assertThatThrownBy(() -> insertOrderRow("Cliente", "SPX", "10.00"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("CHECK constraint failed: length(address_state) = 2");
    }

    @Test
    void shouldRejectInsert_whenTotalIsNegative() {
        // When / Then
        assertThatThrownBy(() -> insertOrderRow("Cliente", "SP", "-0.01"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("CHECK constraint failed: total >= 0");
    }

    @Test
    void shouldRejectItemRow_whenDescriptionIsEmpty() {
        // Given
        Long orderId = persistOrder(ana, 1001L, NOW, 1).getId();

        // When / Then
        assertThatThrownBy(() -> insertItemRow(orderId, "", 1, "10.00", "10.00"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("CHECK constraint failed: length(description) > 0");
    }

    @Test
    void shouldRejectItemRow_whenQuantityIsZero() {
        // Given
        Long orderId = persistOrder(ana, 1001L, NOW, 1).getId();

        // When / Then
        assertThatThrownBy(() -> insertItemRow(orderId, "Item", 0, "10.00", "0.00"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("CHECK constraint failed: quantity >= 1");
    }

    @Test
    void shouldRejectItemRow_whenUnitPriceIsBelowOneCent() {
        // Given
        Long orderId = persistOrder(ana, 1001L, NOW, 1).getId();

        // When / Then
        assertThatThrownBy(() -> insertItemRow(orderId, "Item", 1, "0.00", "0.00"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("CHECK constraint failed: unit_price >= 0.01");
    }

    @Test
    void shouldRejectItemRow_whenSubtotalIsNegative() {
        // Given
        Long orderId = persistOrder(ana, 1001L, NOW, 1).getId();

        // When / Then
        assertThatThrownBy(() -> insertItemRow(orderId, "Item", 1, "10.00", "-0.01"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("CHECK constraint failed: subtotal >= 0");
    }
}
