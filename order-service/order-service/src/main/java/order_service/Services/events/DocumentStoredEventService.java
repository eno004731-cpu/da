package order_service.Services.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import order_service.Dto.payload.DocumentStoredPayload;
import order_service.EntityAndRepo.document.OrderDocumentMetadataEntity;
import order_service.EntityAndRepo.document.OrderDocumentMetadataRepo;
import order_service.EntityAndRepo.events.incoming.IncomingEventEntity;
import order_service.EntityAndRepo.events.incoming.IncomingEventRepo;
import order_service.EntityAndRepo.order.OrderRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentStoredEventService {
    private final IncomingEventRepo incomingEventRepo;
    private final OrderDocumentMetadataRepo documentMetadataRepo;
    private final OrderRepo orderRepo;
    private final ObjectMapper objectMapper;
    private final EventStatusService eventStatusService;

    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroup;

    @Transactional
    public void handleDocumentStored(ConsumerRecord<String, DocumentStoredPayload> record) {
        DocumentStoredPayload payload = record.value();
        if (payload == null || payload.getEventId() == null) {
            log.warn("Document stored event skipped because payload or eventId is empty topic={} partition={} offset={}",
                    record.topic(), record.partition(), record.offset());
            return;
        }
        if (incomingEventRepo.existsByEventId(payload.getEventId())) {
            log.info("Document stored event skipped because event already processed eventId={}", payload.getEventId());
            return;
        }

        IncomingEventEntity incomingEvent = saveReceivedEvent(record, payload);
        if (payload.getOrderId() == null || payload.getDocumentId() == null || payload.getDocumentId().isBlank()) {
            eventStatusService.saveDeadIncomingEvent(incomingEvent, "В событии document.stored отсутствует orderId или documentId");
            return;
        }
        if (!orderRepo.existsById(payload.getOrderId())) {
            eventStatusService.saveDeadIncomingEvent(incomingEvent, "Заказ для document.stored не найден");
            return;
        }
        if (documentMetadataRepo.existsByDocumentId(payload.getDocumentId())) {
            eventStatusService.saveProcessedIncomingEvent(incomingEvent);
            return;
        }

        documentMetadataRepo.save(toMetadataEntity(payload));
        eventStatusService.saveProcessedIncomingEvent(incomingEvent);
    }

    private IncomingEventEntity saveReceivedEvent(ConsumerRecord<String, DocumentStoredPayload> record, DocumentStoredPayload payload) {
        IncomingEventEntity incomingEvent = new IncomingEventEntity();
        incomingEvent.setEventId(payload.getEventId());
        incomingEvent.setTopic(record.topic());
        incomingEvent.setPartitionNo(record.partition());
        incomingEvent.setMessageOffset(record.offset());
        incomingEvent.setConsumerGroup(consumerGroup);
        incomingEvent.setAggregateId(payload.getOrderId());
        incomingEvent.setEventType("DOCUMENT_STORED");
        incomingEvent.setPayload(objectMapper.valueToTree(payload));
        incomingEvent.setStatus("RECEIVED");
        incomingEvent.setRetryCount(0);
        incomingEvent.setReceivedAt(LocalDateTime.now());
        return incomingEventRepo.save(incomingEvent);
    }

    private OrderDocumentMetadataEntity toMetadataEntity(DocumentStoredPayload payload) {
        LocalDateTime now = LocalDateTime.now();
        OrderDocumentMetadataEntity entity = new OrderDocumentMetadataEntity();
        entity.setDocumentId(payload.getDocumentId());
        entity.setOrderId(payload.getOrderId());
        entity.setUploadedByUserId(payload.getUploadedByUserId());
        entity.setFileName(payload.getFileName());
        entity.setMimeType(payload.getMimeType());
        entity.setSizeBytes(payload.getSizeBytes());
        entity.setUploadedAt(LocalDateTime.parse(payload.getUploadedAt()));
        entity.setIsDeleted(Boolean.TRUE.equals(payload.getIsDeleted()));
        entity.setDeletedAt(null);
        entity.setMetadata(objectMapper.valueToTree(payload));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }
}
