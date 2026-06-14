package order_service.services.events;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;
import order_service.services.catalog.SendEventForGetServiceName;

@Component
@RequiredArgsConstructor
public class OutboxWakeUpListener {
    private final SendEventForGetServiceName sendEventForGetServiceName;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOutboxWakeUp(OutboxWakeUpEvent event) {
        // После commit можно безопасно читать outbox из БД и пытаться сразу отправить пачку.
        sendEventForGetServiceName.processAvailableEvents();
    }
}
