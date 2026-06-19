package order_service.services.events.outbox;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import order_service.persistence.events.incoming.IncomingEventEntity;
import order_service.persistence.events.incoming.IncomingEventRepo;
import order_service.persistence.events.incoming.IncomingEventEntity.Status;
import order_service.persistence.events.outbox.OutboxEventEntity;
import order_service.persistence.events.outbox.OutboxEventRepo;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventStatusService {
    private final OutboxEventRepo outboxEventRepo;
    private final IncomingEventRepo incomingEventRepo;

    @Transactional
    public void savePublishedEvent(OutboxEventEntity event){
        event.setStatus("PUBLISHED");
        event.setRetryCount(0);
        event.setPublishedAt(LocalDateTime.now());
        event.setLastError(null);
        event.setNextRetryAt(null);
        outboxEventRepo.save(event);
        log.info("Outbox event marked as published eventId={} eventType={}",
                event.getId(), event.getEventType());
    }

    @Transactional
    public void saveFailedEvent(OutboxEventEntity event,String e){
        event.setLastError(e);
        event.setRetryCount(event.getRetryCount()+1);
        if (event.getRetryCount()<5) {
            event.setStatus("FAILED");
            event.setNextRetryAt(LocalDateTime.now().plusSeconds(5));
        }else{
            event.setStatus("DEAD");
            event.setNextRetryAt(null);
        }
        outboxEventRepo.save(event);
        log.warn("Outbox event publishing failed eventId={} eventType={} status={} retryCount={} error={}",
                event.getId(), event.getEventType(), event.getStatus(), event.getRetryCount(), e);
    }

    @Transactional
    public void saveDeadEvent(OutboxEventEntity event,String e){
        event.setLastError(e);
        event.setNextRetryAt(null);
        event.setRetryCount(event.getRetryCount()+1);
        event.setStatus("DEAD");
        outboxEventRepo.save(event);
        log.error("Outbox event marked as dead eventId={} eventType={} retryCount={} error={}",
                event.getId(), event.getEventType(), event.getRetryCount(), e);
    }

    @Transactional
    public void saveProcessedIncomingEvent(IncomingEventEntity event) {
        event.setStatus(Status.PROCESSED);
        event.setProcessedAt(LocalDateTime.now());
        event.setLastError(null);
        event.setNextRetryAt(null);
        incomingEventRepo.save(event);
        log.info("Incoming event marked as processed eventId={} eventType={}",
                event.getEventId(), event.getEventType());
    }

    @Transactional
    public void saveDeadIncomingEvent(IncomingEventEntity event, String errorMessage) {
        event.setStatus(Status.DEAD);
        event.setProcessedAt(LocalDateTime.now());
        event.setRetryCount(event.getRetryCount() + 1);
        event.setLastError(errorMessage);
        event.setNextRetryAt(null);
        incomingEventRepo.save(event);
        log.error("Incoming event marked as dead eventId={} eventType={} retryCount={} error={}",
                event.getEventId(), event.getEventType(), event.getRetryCount(), errorMessage);
    }

     @Transactional
    public void saveOnDeleteIncomingEvent(IncomingEventEntity event, String errorMessage) {
        event.setStatus(Status.ON_DELETE);
        event.setRetryCount(event.getRetryCount() + 1);
         if (event.getRetryCount()<5) {
            event.setStatus(Status.ON_DELETE);
            event.setNextRetryAt(LocalDateTime.now().plusSeconds(5));
        }else{
            event.setStatus(Status.DEAD);
            event.setNextRetryAt(null);
        }
        event.setLastError(errorMessage);
        incomingEventRepo.save(event);
        log.error("Failed to deserialize incoming document event payload incomingEventId={} eventId={}",
                    event.getId(), event.getEventId(), errorMessage);
    }
}
