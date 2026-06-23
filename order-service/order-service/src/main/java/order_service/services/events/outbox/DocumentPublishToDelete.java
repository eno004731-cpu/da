package order_service.services.events.outbox;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import order_service.dto.payload.DocumentStoredPayload;
import order_service.dto.payload.DocumentToDeletePayload;
import order_service.persistence.events.outbox.OutboxEventEntity;
import order_service.persistence.events.outbox.OutboxEventRepo;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentPublishToDelete {
    private final ObjectMapper objectMapper;
    private final OutboxEventRepo outboxEventRepo;
    private final EventStatusService eventStatusService;
    private final KafkaTemplate<String,DocumentToDeletePayload> kafkaTemplate;
    @Value("${APP_KAFKA_TOPIC_DOCUMENT_DELETE_REQUESTED:document.delete-requested}")
    private String topic;

    @Scheduled(fixedDelay = 10000)
    public void sendDoc(){
        List<OutboxEventEntity> events= outboxEventRepo.findTop100ByStatusOrderByCreatedAtAsc("ON_DELETE");
        log.debug("Found document deletion outbox events count={} topic={}", events.size(), topic);
        for (OutboxEventEntity outboxEventEntity : events) {
            sendEvent(outboxEventEntity);
        }


    }
    private void sendEvent(OutboxEventEntity event){
        DocumentStoredPayload payload = getPayload(event);
        if (payload == null) {
            return ;
        }
        if (payload.getDocumentId() == null || payload.getDocumentId().isBlank()) {
            eventStatusService.saveDeadEvent(event,
                    "В событии удаления документа отсутствует documentId");
            return;
        }
        if (payload.getOrderId() == null) {
            eventStatusService.saveDeadEvent(event,
                    "В событии удаления документа отсутствует orderId");
            return;
        }

        DocumentToDeletePayload payload2 = new DocumentToDeletePayload();
        payload2.setDocumentId(payload.getDocumentId());
        payload2.setEventId(event.getId());
        payload2.setOrderId(payload.getOrderId());
        try {
            kafkaTemplate.send(topic,payload2.getOrderId().toString(),payload2).whenComplete((result,error)->{
                if (error ==null) {
                    eventStatusService.savePublishedEvent(event);
                }else{
                    eventStatusService.saveFailedEvent(event, error.toString());
                }
            });
        } catch (Exception e) {
            eventStatusService.saveFailedEvent(event, e.toString());
        }

    }
    private DocumentStoredPayload getPayload(OutboxEventEntity event){
        DocumentStoredPayload payload;
        try {
            payload = objectMapper.treeToValue(event.getPayload(), DocumentStoredPayload.class);
            log.debug("Document deletion event payload deserialized eventId={}", event.getId());
            return payload;
        } catch (Exception e) {
            eventStatusService.saveFailedEvent(event, e.toString());
            return null;
        }
    }
}
