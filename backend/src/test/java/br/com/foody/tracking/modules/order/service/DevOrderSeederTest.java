package br.com.foody.tracking.modules.order.service;

import static br.com.foody.tracking.modules.order.OrderFixtures.APP_ZONE;
import static br.com.foody.tracking.modules.order.OrderFixtures.NOW;
import static br.com.foody.tracking.modules.order.OrderFixtures.NOW_INSTANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.foody.tracking.modules.auth.entity.User;
import br.com.foody.tracking.modules.auth.enums.Role;
import br.com.foody.tracking.modules.auth.repository.UserRepository;
import br.com.foody.tracking.modules.order.entity.Order;
import br.com.foody.tracking.modules.order.entity.OrderStatusHistory;
import br.com.foody.tracking.modules.order.enums.OrderStatus;
import br.com.foody.tracking.modules.order.repository.OrderRepository;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class DevOrderSeederTest {

  private static final String ADMIN_EMAIL = "admin@foody.local";

  private final UserRepository userRepository = mock(UserRepository.class);
  private final OrderRepository orderRepository = mock(OrderRepository.class);
  private final Clock clock = Clock.fixed(NOW_INSTANT, APP_ZONE);
  private final User admin = new User("Admin", ADMIN_EMAIL, "hash", Role.ADMIN, NOW);

  @Test
  void shouldSeedOrdersCoveringEveryStatusWithLegalHistories_whenAdminOwnsNoOrder() {
    // Given
    when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(admin));
    when(orderRepository.existsByOwnerPublicId(admin.getPublicId())).thenReturn(false);
    AtomicLong code = new AtomicLong(1000);
    when(orderRepository.nextCode()).thenAnswer(invocation -> code.incrementAndGet());

    // When
    seeder(" Admin@Foody.local ").run(new DefaultApplicationArguments());

    // Then
    ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
    verify(orderRepository, atLeastOnce()).saveAndFlush(saved.capture());
    List<Order> orders = saved.getAllValues();
    assertThat(orders).hasSizeBetween(6, 8);
    assertThat(orders).extracting(Order::getStatus).contains(OrderStatus.values());
    assertThat(orders).extracting(Order::getCode).doesNotHaveDuplicates();
    assertThat(orders).allSatisfy(order -> assertThat(order.getOwner()).isSameAs(admin));
    assertThat(orders).allSatisfy(DevOrderSeederTest::assertLegalHistory);
  }

  @Test
  void shouldDoNothing_whenAdminAlreadyOwnsOrders() {
    // Given a second run: the first one left orders behind
    when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(admin));
    when(orderRepository.existsByOwnerPublicId(admin.getPublicId())).thenReturn(true);

    // When
    seeder(ADMIN_EMAIL).run(new DefaultApplicationArguments());

    // Then
    verify(orderRepository, never()).nextCode();
    verify(orderRepository, never()).saveAndFlush(any());
  }

  @Test
  void shouldDoNothing_whenAdminDoesNotExist() {
    // Given
    when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.empty());

    // When
    seeder(ADMIN_EMAIL).run(new DefaultApplicationArguments());

    // Then
    verifyNoInteractions(orderRepository);
  }

  @Test
  void shouldDoNothing_whenAdminEmailBelongsToUser() {
    // Given
    User user = new User("Ana", ADMIN_EMAIL, "hash", Role.USER, NOW);
    when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(user));

    // When
    seeder(ADMIN_EMAIL).run(new DefaultApplicationArguments());

    // Then
    verifyNoInteractions(orderRepository);
  }

  @Test
  void shouldDoNothing_whenNoAdminIsConfigured() {
    // When
    seeder("  ").run(new DefaultApplicationArguments());

    // Then
    verifyNoInteractions(userRepository, orderRepository);
  }

  @ParameterizedTest
  @CsvSource({"dev, true", "test, false", "prod, false"})
  void shouldRegisterSeederOnlyInDev_whenProfileIsActive(String profile, boolean expected) {
    new ApplicationContextRunner()
        .withPropertyValues("spring.profiles.active=" + profile)
        .withBean(UserRepository.class, () -> userRepository)
        .withBean(OrderRepository.class, () -> orderRepository)
        .withBean(Clock.class, () -> clock)
        .withUserConfiguration(DevOrderSeeder.class)
        .run(
            context -> {
              // Then
              assertThat(context.getBeansOfType(DevOrderSeeder.class)).hasSize(expected ? 1 : 0);
            });
  }

  private DevOrderSeeder seeder(String adminEmail) {
    return new DevOrderSeeder(adminEmail, userRepository, orderRepository, clock);
  }

  // Born RECEBIDO, then only allowed transitions by the admin, strictly increasing and in the past.
  private static void assertLegalHistory(Order order) {
    List<OrderStatusHistory> history = order.getStatusHistory();
    assertThat(history.get(0).getFromStatus()).isNull();
    assertThat(history.get(0).getToStatus()).isEqualTo(OrderStatus.RECEBIDO);
    for (int i = 1; i < history.size(); i++) {
      OrderStatusHistory previous = history.get(i - 1);
      OrderStatusHistory current = history.get(i);
      assertThat(current.getFromStatus()).isEqualTo(previous.getToStatus());
      assertThat(previous.getToStatus().canTransitionTo(current.getToStatus())).isTrue();
      assertThat(current.getChangedAt()).isAfter(previous.getChangedAt());
    }
    assertThat(history.get(history.size() - 1).getToStatus()).isEqualTo(order.getStatus());
    assertThat(history)
        .allSatisfy(
            entry -> {
              assertThat(entry.getChangedBy()).isEqualTo(order.getOwner().getPublicId());
              assertThat(entry.getChangedAt().toInstant()).isBefore(NOW_INSTANT);
            });
  }
}
