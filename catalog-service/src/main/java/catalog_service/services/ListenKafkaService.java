package catalog_service.services;

import java.time.LocalDateTime;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import catalog_service.dto.payload.GetServiceNamePayload;
import catalog_service.entityAndRepo.inbox.InboxEventEntity;
import catalog_service.entityAndRepo.inbox.InboxEventRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ListenKafkaService {
    private final ObjectMapper objectMapper;
    private final InboxEventRepo inboxEventRepo;
    private final EventService eventService;
    private final ApplicationEventPublisher applicationEventPublisher;
    
    @KafkaListener(
            topics = "${app.kafka.topics.catalog-get-service-name-request}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void listenOrderService(ConsumerRecord<String,GetServiceNamePayload> record){
        saveEvent(record);
    }

    public void saveEvent(ConsumerRecord<String,GetServiceNamePayload> record){
        GetServiceNamePayload payload = record.value();
        if (payload == null) {
            log.warn("payload is empty");
            return;
        }
        if (payload.getServiceCode() == null || payload.getServiceCode().isBlank()) {
            log.error("ServiceCode is empty eventId={}", payload.getEventId());
            return;
        }
        if (payload.getOrderId() == null) {
            log.error("OrderId is empty eventId={}", payload.getEventId());
            return;
        }
        if (inboxEventRepo.existsByEventId(payload.getEventId())) {
            log.info("Catalog response skipped because event already processed eventId={}", payload.getEventId());
            return;
        }
        saveEvent(payload);

    }
    private void saveEvent(GetServiceNamePayload payload){
        InboxEventEntity event = new InboxEventEntity();
        event.setEventId(payload.getEventId());
        event.setEventType("GetServiceName");
        event.setCreatedAt(LocalDateTime.now());
        try {
            // В inbox сохраняем исходное сообщение, чтобы дальше можно было безопасно повторить обработку.
            event.setPayload(objectMapper.valueToTree(payload));
        } catch (Exception e) {
            eventService.saveFailedEvent(event, e.toString());
            return;
        }
        event.setStatus("RECEIVED");
        event.setRetryCount(0);
        inboxEventRepo.save(event);
        // Публикуем Spring-событие внутри транзакции, чтобы listener проснулся только после commit.
        applicationEventPublisher.publishEvent(new InboxSavedEvent(event.getId()));
    }
}
