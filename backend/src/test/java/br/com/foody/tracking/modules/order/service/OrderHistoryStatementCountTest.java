package br.com.foody.tracking.modules.order.service;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.foody.tracking.modules.auth.entity.User;
import br.com.foody.tracking.modules.auth.enums.Role;
import br.com.foody.tracking.modules.auth.repository.UserRepository;
import br.com.foody.tracking.modules.auth.security.AuthenticatedUser;
import br.com.foody.tracking.modules.order.dto.StatusHistoryResponseDTO;
import br.com.foody.tracking.modules.order.entity.Customer;
import br.com.foody.tracking.modules.order.entity.DeliveryAddress;
import br.com.foody.tracking.modules.order.entity.Order;
import br.com.foody.tracking.modules.order.entity.OrderItem;
import br.com.foody.tracking.modules.order.enums.OrderStatus;
import br.com.foody.tracking.modules.order.mapper.OrderMapperImpl;
import br.com.foody.tracking.modules.order.repository.OrderRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

// The history endpoint must cost a fixed number of statements: the scoped order, its history rows
// and one query for the actors' names, whatever the number of rows or actors.
@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import({OrderQueryService.class, OrderFinder.class, OrderMapperImpl.class})
class OrderHistoryStatementCountTest {

  private static final ZoneId APP_ZONE = ZoneId.of("America/Sao_Paulo");
  private static final OffsetDateTime NOW =
      OffsetDateTime.ofInstant(Instant.parse("2026-10-21T17:13:27Z"), APP_ZONE);

  @Autowired private OrderQueryService orderQueryService;

  @Autowired private OrderRepository orderRepository;

  @Autowired private UserRepository userRepository;

  @Autowired private EntityManager entityManager;

  private User ana;

  private User admin;

  @BeforeEach
  void setUp() {
    ana =
        userRepository.saveAndFlush(
            new User("Ana Souza", "ana@exemplo.com", "hash", Role.USER, NOW));
    admin =
        userRepository.saveAndFlush(
            new User("Admin", "admin@exemplo.com", "hash", Role.ADMIN, NOW));
  }

  @Test
  void shouldUseThreeStatements_whenHistoryHasOneOrManyRowsAndActors() {
    // Given one order with only the creation row and one with three rows by two actors
    Order fresh = persistOrder(9001L);
    Order advanced = persistOrder(9002L);
    advanced.changeStatus(OrderStatus.EM_PREPARO, admin.getPublicId(), NOW.plusMinutes(5));
    advanced.changeStatus(OrderStatus.SAIU_PARA_ENTREGA, ana.getPublicId(), NOW.plusMinutes(10));
    orderRepository.saveAndFlush(advanced);
    entityManager.clear();
    AuthenticatedUser actor = new AuthenticatedUser(ana.getPublicId(), Role.USER);

    // When
    List<List<StatusHistoryResponseDTO>> results = new ArrayList<>();
    long freshStatements =
        statementsFor(() -> results.add(orderQueryService.history(fresh.getPublicId(), actor)));
    entityManager.clear();
    long advancedStatements =
        statementsFor(() -> results.add(orderQueryService.history(advanced.getPublicId(), actor)));

    // Then
    assertThat(freshStatements).isEqualTo(3);
    assertThat(advancedStatements).isEqualTo(freshStatements);
    assertThat(results.get(0))
        .extracting(StatusHistoryResponseDTO::changedByName)
        .containsExactly("Ana Souza");
    assertThat(results.get(1))
        .extracting(StatusHistoryResponseDTO::changedByName)
        .containsExactly("Ana Souza", "Admin", "Ana Souza");
  }

  private long statementsFor(Runnable call) {
    Statistics statistics =
        entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
    statistics.clear();
    call.run();
    return statistics.getPrepareStatementCount();
  }

  private Order persistOrder(long code) {
    return orderRepository.saveAndFlush(
        Order.create(
            ana,
            new Customer("Cliente " + code, null, null),
            List.of(new OrderItem("Item", 1, new BigDecimal("10.00"))),
            new DeliveryAddress(
                "Rua José Neves",
                "590",
                null,
                "Vila São Paulo",
                "São Paulo",
                "SP",
                "04650-140",
                null,
                null),
            null,
            code,
            NOW));
  }
}
