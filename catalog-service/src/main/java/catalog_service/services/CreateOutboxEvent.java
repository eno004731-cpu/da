package catalog_service.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import catalog_service.catalog.ServiceEntity;
import catalog_service.catalog.ServiceRepository;
import catalog_service.dto.payload.GetServiceNamePayload;
import catalog_service.entityAndRepo.inbox.InboxEventEntity;
import catalog_service.entityAndRepo.inbox.InboxEventRepo;
import catalog_service.entityAndRepo.outbox.OutboxEventEntity;
import catalog_service.entityAndRepo.outbox.OutboxEventRepo;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateOutboxEvent {
    private final EventService eventService;
    private final InboxEventRepo inboxEventRepo;
    private final ObjectMapper objectMapper;
    private final OutboxEventRepo outboxEventRepo;
    private final ServiceRepository serviceRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final AtomicBoolean atomicBoolean = new AtomicBoolean();

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void createOutboxEvent1(){
        createOutboxEvent();
    }

    @Transactional
    public void createOutboxEvent(){
        if (!atomicBoolean.compareAndSet(false, true)) {
            return;
        }
        try{
            List<InboxEventEntity> newEvents = inboxEventRepo.findTop100ByStatusOrderByCreatedAtAsc("RECEIVED");
            List<InboxEventEntity> failedEvents = inboxEventRepo
                .findTop100ByStatusAndNextRetryAtBeforeOrderByNextRetryAtAsc(
                    "FAILED",
                    LocalDateTime.now());
            createOutboxEvents(newEvents);
            createOutboxEvents(failedEvents);
        }finally{
            atomicBoolean.set(false);
        }
    }
    private void createOutboxEvents(List<InboxEventEntity> events){
        for (InboxEventEntity event : events) {
            GetServiceNamePayload payload = getPayload(event);
            if (payload == null) {
                continue;
            }
            Optional<ServiceEntity> service = serviceRepository.findByCode(payload.getServiceCode());
            if (service.isEmpty()) {
                eventService.saveDeadEvent(event, "Not found service");
                continue;
            }
            ServiceEntity service1 = service.get();
            payload.setServiceName(service1.getName());
            saveNewOutEvent(event, payload);
            eventService.saveProcessedvent(event);
        }
    }

    private void saveNewOutEvent(InboxEventEntity event,GetServiceNamePayload payload){
        OutboxEventEntity outEvent = new OutboxEventEntity();
        outEvent.setAggregateId(payload.getOrderId());
        outEvent.setCreatedAt(LocalDateTime.now());
        outEvent.setEventType("GetServiceName");
        try {
            // В outbox должен уйти уже обогащённый payload с serviceName.
            outEvent.setPayload(objectMapper.valueToTree(payload));
        } catch (Exception e) {
            eventService.saveFailedEvent(event, e.toString());
            return;
        }
        outEvent.setStatus("NEW");
        outEvent.setRetryCount(0);
        outboxEventRepo.save(outEvent);
        // Отдельный тип события помогает listener понять, что уже можно будить Kafka publisher.
        applicationEventPublisher.publishEvent(new OutboxSavedEvent(outEvent.getId()));
    }
    private GetServiceNamePayload getPayload(InboxEventEntity event){
        GetServiceNamePayload payload = null;
        try {
            payload = objectMapper.treeToValue(event.getPayload(), GetServiceNamePayload.class);
        } catch (Exception e) {
            eventService.saveFailedEvent(event, e.toString());
            return null;
        }
        return payload;
    }
}
