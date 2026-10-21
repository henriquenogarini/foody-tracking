package br.com.foody.tracking.modules.order.enums;

import static br.com.foody.tracking.modules.order.enums.OrderStatus.CANCELADO;
import static br.com.foody.tracking.modules.order.enums.OrderStatus.EM_PREPARO;
import static br.com.foody.tracking.modules.order.enums.OrderStatus.ENTREGUE;
import static br.com.foody.tracking.modules.order.enums.OrderStatus.RECEBIDO;
import static br.com.foody.tracking.modules.order.enums.OrderStatus.SAIU_PARA_ENTREGA;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class OrderStatusTest {

  private static final Set<String> ALLOWED =
      Set.of(
          "RECEBIDO>EM_PREPARO",
          "RECEBIDO>CANCELADO",
          "EM_PREPARO>SAIU_PARA_ENTREGA",
          "EM_PREPARO>CANCELADO",
          "SAIU_PARA_ENTREGA>ENTREGUE");

  static Stream<Arguments> fullMatrix() {
    return Stream.of(OrderStatus.values())
        .flatMap(
            from ->
                Stream.of(OrderStatus.values())
                    .map(to -> Arguments.of(from, to, ALLOWED.contains(from + ">" + to))));
  }

  @ParameterizedTest(name = "{0} -> {1} allowed={2}")
  @MethodSource("fullMatrix")
  void shouldMatchTheStateMachine_whenCheckingEveryPairOfStatuses(
      OrderStatus from, OrderStatus to, boolean expected) {
    // When
    boolean allowed = from.canTransitionTo(to);

    // Then
    assertThat(allowed).isEqualTo(expected);
  }

  @Test
  void shouldCoverTheWholeMatrix_whenGeneratingCases() {
    // When / Then
    assertThat(fullMatrix()).hasSize(25);
    assertThat(fullMatrix().filter(arguments -> (boolean) arguments.get()[2])).hasSize(5);
  }

  @ParameterizedTest(name = "{0} terminal={1}")
  @CsvSource({
    "RECEBIDO, false",
    "EM_PREPARO, false",
    "SAIU_PARA_ENTREGA, false",
    "ENTREGUE, true",
    "CANCELADO, true"
  })
  void shouldReportTerminal_whenStatusHasNoOutgoingTransition(
      OrderStatus status, boolean terminal) {
    // When / Then
    assertThat(status.isTerminal()).isEqualTo(terminal);
  }

  @Test
  void shouldRejectNullTarget_whenCheckingTransition() {
    // When / Then
    assertThat(RECEBIDO.canTransitionTo(null)).isFalse();
  }

  @Test
  void shouldListValuesInLifecycleOrder_whenEnumerated() {
    // When / Then
    assertThat(OrderStatus.values())
        .containsExactly(RECEBIDO, EM_PREPARO, SAIU_PARA_ENTREGA, ENTREGUE, CANCELADO);
  }
}
