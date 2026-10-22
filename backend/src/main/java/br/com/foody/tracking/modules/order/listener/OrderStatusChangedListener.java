package br.com.foody.tracking.modules.order.listener;

import br.com.foody.tracking.modules.order.event.OrderStatusChangedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public final class OrderStatusChangedListener {

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
