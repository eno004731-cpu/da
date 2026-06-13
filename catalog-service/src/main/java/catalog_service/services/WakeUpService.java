package catalog_service.services;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class WakeUpService {
    private final SendServiceName sendServiceName;
    private final CreateOutboxEvent createOutboxEvent;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void outboxWakeUp(OutboxSavedEvent event) {
        // Этот параметр и есть маркер: listener сработал именно после сохранения outbox-сообщения.
        log.info("Wake up Kafka publisher after outbox save id={}", event.outboxEventId());
        sendServiceName.sendEvent();
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void createOutboxEventWakeUp(InboxSavedEvent event){
        // Этот параметр и есть маркер: listener сработал именно после сохранения inbox-сообщения.
        log.info("Wake up outbox creator after inbox save id={}", event.inboxEventId());
        createOutboxEvent.createOutboxEvent();
    }
}
