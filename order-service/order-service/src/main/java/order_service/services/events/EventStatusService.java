package order_service.services.events;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import order_service.persistence.events.incoming.IncomingEventEntity;
import order_service.persistence.events.incoming.IncomingEventRepo;
import order_service.persistence.events.outbox.OutboxEventEntity;
import order_service.persistence.events.outbox.OutboxEventRepo;

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
    }

    @Transactional
    public void saveFailedEvent(OutboxEventEntity event,String e){
        event.setLastError(e);
        event.setNextRetryAt(LocalDateTime.now().plusSeconds(5));
        event.setRetryCount(event.getRetryCount()+1);
        if (event.getRetryCount()<5) {
            event.setStatus("FAILED");
        }else{
            event.setStatus("DEAD");
            event.setNextRetryAt(null);
        }
        outboxEventRepo.save(event);
    }

    @Transactional
    public void saveDeadEvent(OutboxEventEntity event,String e){
        event.setLastError(e);
        event.setNextRetryAt(null);
        event.setRetryCount(event.getRetryCount()+1);
        event.setStatus("DEAD");
        outboxEventRepo.save(event);
    }

    @Transactional
    public void saveProcessedIncomingEvent(IncomingEventEntity event) {
        event.setStatus("PROCESSED");
        event.setProcessedAt(LocalDateTime.now());
        event.setLastError(null);
        event.setNextRetryAt(null);
        incomingEventRepo.save(event);
    }

    @Transactional
    public void saveDeadIncomingEvent(IncomingEventEntity event, String errorMessage) {
        event.setStatus("DEAD");
        event.setProcessedAt(LocalDateTime.now());
        event.setRetryCount(event.getRetryCount() + 1);
        event.setLastError(errorMessage);
        event.setNextRetryAt(null);
        incomingEventRepo.save(event);
    }
}
