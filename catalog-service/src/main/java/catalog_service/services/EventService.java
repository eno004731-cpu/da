package catalog_service.services;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import catalog_service.entityAndRepo.inbox.InboxEventEntity;
import catalog_service.entityAndRepo.inbox.InboxEventRepo;
import catalog_service.entityAndRepo.outbox.OutboxEventEntity;
import catalog_service.entityAndRepo.outbox.OutboxEventRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {
    private final InboxEventRepo inboxEventRepo;
    private final OutboxEventRepo outboxEventRepo;
    @Transactional
    public void saveDeadEvent(InboxEventEntity event, String errorMessage) {
        event.setStatus("DEAD");
        event.setRetryCount(5);
        event.setLastError(errorMessage);
        event.setNextRetryAt(null);
        event.setProcessedAt(null);
        inboxEventRepo.save(event);

        log.error(
                "Inbox event moved to DEAD status eventId={} eventType={} error={}",
                event.getEventId(),
                event.getEventType(),
                errorMessage);
    }
    @Transactional
    public void saveDeadEvent(OutboxEventEntity event, String errorMessage) {
        event.setStatus("DEAD");
        event.setRetryCount(5);
        event.setLastError(errorMessage);
        event.setNextRetryAt(null);
        event.setPublishedAt(null);
        outboxEventRepo.save(event);

        log.error(
                "Outbox event moved to DEAD status eventId={} eventType={} error={}",
                event.getId(),
                event.getEventType(),
                errorMessage);
    }
    @Transactional
    public void saveFailedEvent(InboxEventEntity event, String errorMessage) {
        int retryCount = event.getRetryCount() == null ? 0 : event.getRetryCount();

        event.setLastError(errorMessage);
        event.setNextRetryAt(LocalDateTime.now().plusSeconds(5));
        event.setRetryCount(retryCount + 1);

        // Повторяем обработку ограниченное число раз, потом помечаем событие как DEAD.
        if (event.getRetryCount() < 5) {
            event.setStatus("FAILED");
        } else {
            event.setStatus("DEAD");
            event.setNextRetryAt(null);
        }

        inboxEventRepo.save(event);

        log.warn(
                "Inbox event saved with status={} eventId={} eventType={} retryCount={} nextRetryAt={} error={}",
                event.getStatus(),
                event.getEventId(),
                event.getEventType(),
                event.getRetryCount(),
                event.getNextRetryAt(),
                errorMessage);
    }
    @Transactional
    public void saveFailedEvent(OutboxEventEntity event, String errorMessage) {
        int retryCount = event.getRetryCount() == null ? 0 : event.getRetryCount();

        event.setLastError(errorMessage);
        event.setNextRetryAt(LocalDateTime.now().plusSeconds(5));
        event.setRetryCount(retryCount + 1);

        // Повторяем обработку ограниченное число раз, потом помечаем событие как DEAD.
        if (event.getRetryCount() < 5) {
            event.setStatus("FAILED");
        } else {
            event.setStatus("DEAD");
            event.setNextRetryAt(null);
        }

        outboxEventRepo.save(event);

        log.warn(
                "Outbox event saved with status={} eventType={} retryCount={} nextRetryAt={} error={}",
                event.getStatus(),
                event.getEventType(),
                event.getRetryCount(),
                event.getNextRetryAt(),
                errorMessage);
    }
    @Transactional
    public void saveProcessedvent(InboxEventEntity event) {
        event.setStatus("PROCESSED");
        event.setProcessedAt(LocalDateTime.now());
        event.setLastError(null);
        event.setNextRetryAt(null);
        inboxEventRepo.save(event);

        log.info(
                "Inbox event marked as PROCESSED eventId={} eventType={} processedAt={}",
                event.getEventId(),
                event.getEventType(),
                event.getProcessedAt());
    }
    @Transactional
    public void savePublished(OutboxEventEntity event) {
        event.setStatus("PUBLISHED");
        event.setPublishedAt(LocalDateTime.now());
        event.setLastError(null);
        event.setNextRetryAt(null);
        outboxEventRepo.save(event);

        log.info(
                "Outbox event marked as PUBLISHED id={} eventType={} publishedAt={}",
                event.getId(),
                event.getEventType(),
                event.getPublishedAt()
        );
    }
}
