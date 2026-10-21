package br.com.foody.tracking.modules.order.listener;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.foody.tracking.modules.order.enums.OrderStatus;
import br.com.foody.tracking.modules.order.event.OrderStatusChangedEvent;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@ExtendWith(OutputCaptureExtension.class)
@SpringJUnitConfig(OrderStatusChangedListenerTest.Config.class)
class OrderStatusChangedListenerTest {

  @Autowired private ApplicationEventPublisher publisher;

  @Autowired private PlatformTransactionManager transactionManager;

  private static OrderStatusChangedEvent event() {
    return new OrderStatusChangedEvent(
        UUID.randomUUID(),
        1042L,
        OrderStatus.RECEBIDO,
        OrderStatus.EM_PREPARO,
        UUID.randomUUID(),
        OffsetDateTime.parse("2026-10-21T14:13:27-03:00"));
  }

  private static String expectedLine(OrderStatusChangedEvent event) {
    return "Order "
        + event.orderId()
        + " (1042) status changed RECEBIDO -> EM_PREPARO by "
        + event.changedBy();
  }

  @Test
  void shouldLogOnceAfterCommit_whenTransactionCommits(CapturedOutput output) {
    // Given
    OrderStatusChangedEvent event = event();
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);

    // When
    transaction.executeWithoutResult(
        status -> {
          publisher.publishEvent(event);
          // Then (still inside the transaction): nothing yet
          assertThat(output.getOut()).doesNotContain(event.orderId().toString());
        });

    // Then
    assertThat(StringUtils.countOccurrencesOf(output.getOut(), expectedLine(event))).isEqualTo(1);
  }

  @Test
  void shouldNotLog_whenTransactionRollsBack(CapturedOutput output) {
    // Given
    OrderStatusChangedEvent event = event();
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);

    // When
    transaction.executeWithoutResult(
        status -> {
          publisher.publishEvent(event);
          status.setRollbackOnly();
        });

    // Then
    assertThat(output.getOut()).doesNotContain(event.orderId().toString());
  }

  @Test
  void shouldNotLog_whenPublishedOutsideTransaction(CapturedOutput output) {
    // Given
    OrderStatusChangedEvent event = event();

    // When
    publisher.publishEvent(event);

    // Then
    assertThat(output.getOut()).doesNotContain(event.orderId().toString());
  }

  @Configuration
  @EnableTransactionManagement
  @Import(OrderStatusChangedListener.class)
  static class Config {

    @Bean
    PlatformTransactionManager transactionManager() {
      return new NoResourceTransactionManager();
    }
  }

  // Drives the real synchronization lifecycle (commit/rollback callbacks) without a database.
  private static final class NoResourceTransactionManager
      extends AbstractPlatformTransactionManager {

    @Override
    protected Object doGetTransaction() {
      return new Object();
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {}

    @Override
    protected void doCommit(DefaultTransactionStatus status) {}

    @Override
    protected void doRollback(DefaultTransactionStatus status) {}
  }
}
