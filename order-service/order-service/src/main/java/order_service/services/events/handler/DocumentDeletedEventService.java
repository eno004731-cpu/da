package order_service.services.events.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import order_service.dto.payload.DocumentDeletedPayload;
import order_service.persistence.document.OrderDocumentMetadataEntity;
import order_service.persistence.document.OrderDocumentMetadataRepo;
import order_service.persistence.events.incoming.IncomingEventEntity;
import order_service.persistence.events.incoming.IncomingEventRepo;
import order_service.persistence.events.incoming.IncomingEventEntity.Status;
import order_service.services.events.outbox.EventStatusService;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentDeletedEventService {
    private final IncomingEventRepo incomingEventRepo;
    private final OrderDocumentMetadataRepo documentMetadataRepo;
    private final ObjectMapper objectMapper;
    private final EventStatusService eventStatusService;

    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroup;

    @Transactional
    public void handleDocumentDeleted(ConsumerRecord<String, DocumentDeletedPayload> record) {
        DocumentDeletedPayload payload = record.value();
        if (payload == null || payload.getEventId() == null) {
            log.warn("Document deleted event skipped because payload or eventId is empty topic={} partition={} offset={}",
                    record.topic(), record.partition(), record.offset());
            return;
        }
        if (incomingEventRepo.existsByEventId(payload.getEventId())) {
            log.info("Document deleted event skipped because event already processed eventId={}", payload.getEventId());
            return;
        }

        IncomingEventEntity incomingEvent = saveReceivedEvent(record, payload);
        if (payload.getOrderId() == null || payload.getDocumentId() == null || payload.getDocumentId().isBlank()) {
            eventStatusService.saveDeadIncomingEvent(incomingEvent, "В событии document.deleted отсутствует orderId или documentId");
            return;
        }

        documentMetadataRepo.findByOrderIdAndDocumentId(payload.getOrderId(), payload.getDocumentId())
                .ifPresent(metadata -> markDeleted(metadata, payload));
        eventStatusService.saveProcessedIncomingEvent(incomingEvent);
    }

    private IncomingEventEntity saveReceivedEvent(ConsumerRecord<String, DocumentDeletedPayload> record, DocumentDeletedPayload payload) {
        IncomingEventEntity incomingEvent = new IncomingEventEntity();
        incomingEvent.setEventId(payload.getEventId());
        incomingEvent.setTopic(record.topic());
        incomingEvent.setPartitionNo(record.partition());
        incomingEvent.setMessageOffset(record.offset());
        incomingEvent.setConsumerGroup(consumerGroup);
        incomingEvent.setAggregateId(payload.getOrderId());
        incomingEvent.setEventType("DOCUMENT_DELETED");
        incomingEvent.setPayload(objectMapper.valueToTree(payload));
        incomingEvent.setStatus(Status.RECEIVED);
        incomingEvent.setRetryCount(0);
        incomingEvent.setReceivedAt(LocalDateTime.now());
        return incomingEventRepo.save(incomingEvent);
    }

    private void markDeleted(OrderDocumentMetadataEntity metadata, DocumentDeletedPayload payload) {
        metadata.setIsDeleted(true);
        metadata.setDeletedAt(parseDeletedAt(payload));
        metadata.setUpdatedAt(LocalDateTime.now());
        documentMetadataRepo.save(metadata);
    }

    private LocalDateTime parseDeletedAt(DocumentDeletedPayload payload) {
        if (payload.getDeletedAt() == null || payload.getDeletedAt().isBlank()) {
            return LocalDateTime.now();
        }
        return LocalDateTime.parse(payload.getDeletedAt());
    }
}
