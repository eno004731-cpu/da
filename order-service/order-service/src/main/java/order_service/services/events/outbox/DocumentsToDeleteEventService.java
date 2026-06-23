package order_service.services.events.outbox;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import order_service.dto.payload.DocumentStoredPayload;
import order_service.persistence.events.incoming.IncomingEventEntity;
import order_service.persistence.events.incoming.IncomingEventRepo;
import order_service.persistence.events.outbox.OutboxEventEntity;
import order_service.persistence.events.outbox.OutboxEventRepo;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentsToDeleteEventService {
    private final IncomingEventRepo incomingEventRepo;
    private final ObjectMapper objectMapper;
    private final EventStatusService eventStatusService;
    private final OutboxEventRepo outboxEventRepo;
    @Value("${app.kafka.topics.document-stored}")
    private String topic;

    @Scheduled(fixedDelay = 10000)
    public void deleteDocCuzDeadStatus(){
        log.debug("Searching for document events marked for deletion topic={}", topic);
        for (OutboxEventEntity entity : incomingEventRepo.findTop100ByStatusAndTopicOrderByReceivedAtAsc(
                        IncomingEventEntity.Status.ON_DELETE,
                        topic
                )
                .stream()
                .map(this::saveOutboxEvent).toList()) {
            if (entity == null) {
                log.warn("Skipping document deletion outbox event because it could not be created");
                continue;
            }
            outboxEventRepo.save(entity);
            log.info("Document deletion outbox event saved eventId={} aggregateId={} eventType={}",
                    entity.getId(), entity.getAggregateId(), entity.getEventType());
        }
    }

    private OutboxEventEntity saveOutboxEvent(IncomingEventEntity entity){
        if (entity == null) {
            log.warn("Cannot create document deletion outbox event because incoming event is null");
            return null;
        }
        DocumentStoredPayload payload = getPayload(entity);
        if (payload == null) {
            log.warn("Cannot create document deletion outbox event because payload is invalid incomingEventId={} eventId={}",
                    entity.getId(), entity.getEventId());
            return null;
        }
        OutboxEventEntity event = eventEntity(entity, payload);
        log.debug("Document deletion outbox event created incomingEventId={} eventId={} orderId={}",
                entity.getId(), entity.getEventId(), payload.getOrderId());
        return event;
    }

    private OutboxEventEntity eventEntity(IncomingEventEntity entity,DocumentStoredPayload payload){
        OutboxEventEntity event = new OutboxEventEntity();
        event.setAggregateId(payload.getOrderId());
        event.setEventType("DOCUMETN_TO_DELETE");
        event.setPayload(entity.getPayload());
        event.setStatus("NEW");
        event.setCreatedAt(LocalDateTime.now());
        return event;

    }
    private DocumentStoredPayload getPayload(IncomingEventEntity entity){
        DocumentStoredPayload payload;
        try {
            payload = objectMapper.treeToValue(entity.getPayload(), DocumentStoredPayload.class);
            log.debug("Incoming document event payload deserialized incomingEventId={} eventId={}",
                    entity.getId(), entity.getEventId());
            return payload;
        } catch (Exception e) {
            
            // Сохраняем причину ошибки и прекращаем обработку повреждённого события.
            eventStatusService.saveDeadIncomingEvent(entity, e.toString());
            return null;
        }
    }

}
