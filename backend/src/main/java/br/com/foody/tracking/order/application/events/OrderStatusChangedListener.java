package br.com.foody.tracking.order.application.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public final class OrderStatusChangedListener {

  private static final Logger log = LoggerFactory.getLogger(OrderStatusChangedListener.class);

  // Extension point for a webhook or a queue. AFTER_COMMIT means it only runs once the change is
  // durable, so a rolled-back transition never notifies anyone.
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onStatusChanged(OrderStatusChangedEvent event) {
    log.info(
        "Order {} ({}) status changed {} -> {} by {}",
        event.orderId(),
        event.code(),
        event.fromStatus(),
        event.toStatus(),
        event.changedBy());
  }
}
