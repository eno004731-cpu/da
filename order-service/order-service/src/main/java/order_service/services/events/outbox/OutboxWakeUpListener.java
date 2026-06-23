package order_service.services.events.outbox;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import order_service.services.catalog.SendEventForGetServiceName;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxWakeUpListener {
    private final SendEventForGetServiceName sendEventForGetServiceName;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOutboxWakeUp(OutboxWakeUpEvent event) {
        // После commit можно безопасно читать outbox из БД и пытаться сразу отправить пачку.
        log.debug("Outbox publisher wake-up received eventId={}", event.getOutboxEventId());
        sendEventForGetServiceName.processAvailableEvents();
    }
}
