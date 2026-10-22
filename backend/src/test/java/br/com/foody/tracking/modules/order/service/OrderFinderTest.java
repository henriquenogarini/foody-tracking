package br.com.foody.tracking.modules.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import br.com.foody.tracking.modules.auth.enums.Role;
import br.com.foody.tracking.modules.auth.security.AuthenticatedUser;
import br.com.foody.tracking.modules.order.entity.Order;
import br.com.foody.tracking.modules.order.repository.OrderRepository;
import br.com.foody.tracking.shared.exception.type.OrderNotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrderFinderTest {

  private final OrderRepository orderRepository = mock(OrderRepository.class);
  private final OrderFinder finder = new OrderFinder(orderRepository);

  private final AuthenticatedUser userActor = new AuthenticatedUser(UUID.randomUUID(), Role.USER);
  private final AuthenticatedUser adminActor = new AuthenticatedUser(UUID.randomUUID(), Role.ADMIN);

  @Test
  void shouldUseUnscopedQuery_whenActorIsAdmin() {
    // Given
    UUID publicId = UUID.randomUUID();
    Order order = mock(Order.class);
    when(orderRepository.findByPublicId(publicId)).thenReturn(Optional.of(order));

    // When
    Order found = finder.getVisibleTo(publicId, adminActor);

    // Then
    assertThat(found).isSameAs(order);
    verify(orderRepository).findByPublicId(publicId);
    verifyNoMoreInteractions(orderRepository);
  }

  @Test
  void shouldUseOwnerScopedQuery_whenActorIsUser() {
    // Given
    UUID publicId = UUID.randomUUID();
    Order order = mock(Order.class);
    when(orderRepository.findByPublicIdAndOwnerPublicId(publicId, userActor.id()))
        .thenReturn(Optional.of(order));

    // When
    Order found = finder.getVisibleTo(publicId, userActor);

    // Then
    assertThat(found).isSameAs(order);
    verify(orderRepository).findByPublicIdAndOwnerPublicId(publicId, userActor.id());
    verifyNoMoreInteractions(orderRepository);
  }

  @Test
  void shouldThrowOrderNotFound_whenUserTargetsAnotherUsersOrder() {
    // Given
    UUID publicId = UUID.randomUUID();
    when(orderRepository.findByPublicIdAndOwnerPublicId(publicId, userActor.id()))
        .thenReturn(Optional.empty());

    // When / Then
    assertThatThrownBy(() -> finder.getVisibleTo(publicId, userActor))
        .isInstanceOf(OrderNotFoundException.class);
  }

  @Test
  void shouldThrowOrderNotFound_whenAdminTargetsMissingOrder() {
    // Given
    UUID publicId = UUID.randomUUID();
    when(orderRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

    // When / Then
    assertThatThrownBy(() -> finder.getVisibleTo(publicId, adminActor))
        .isInstanceOf(OrderNotFoundException.class);
  }
}
