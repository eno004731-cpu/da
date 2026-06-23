package order_service.services.events.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import order_service.dto.payload.DocumentStoredPayload;
import order_service.persistence.events.incoming.IncomingEventEntity;
import order_service.persistence.events.incoming.IncomingEventRepo;
import order_service.persistence.events.incoming.IncomingEventEntity.Status;
import order_service.persistence.order.OrderRepo;
import order_service.services.events.outbox.EventStatusService;
import order_service.services.events.outbox.DocumentValidationOutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentStoredEventService {
    private final IncomingEventRepo incomingEventRepo;
    private final OrderRepo orderRepo;
    private final ObjectMapper objectMapper;
    private final EventStatusService eventStatusService;
    private final DocumentValidationOutboxService documentValidationOutboxService;

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

        // Уже удалённый в document-service файл не требует сохранения метаданных или повторного удаления.
        if (Boolean.TRUE.equals(payload.getIsDeleted())) {
            eventStatusService.saveDeadIncomingEvent(incomingEvent, "");
            return;
        }

        // Без documentId нельзя отправить document-service команду на удаление конкретного файла.
        if (payload.getDocumentId() == null || payload.getDocumentId().isBlank()) {
            eventStatusService.saveDeadIncomingEvent(incomingEvent,
                    "В событии document.stored отсутствует documentId");
            return;
        }

        String validationError = validatePayload(payload);
        if (validationError != null) {
            // Документ уже сохранён, но его метаданные нельзя принять — файл должен быть удалён как сиротский.
            eventStatusService.saveOnDeleteIncomingEvent(incomingEvent, validationError);
            return;
        }

        if (!orderRepo.existsByIdAndClientIdAndIsDeletedFalseAndDeletionInProgressFalse(
                payload.getOrderId(),
                payload.getUploadedByUserId()
        )) {
            // Файл относится к несуществующему заказу и должен быть удалён в document-service.
            eventStatusService.saveOnDeleteIncomingEvent(incomingEvent,
                    "Заказ для document.stored не найден");
            return;
        }
        // Inbox и outbox сохраняются в одной транзакции: либо фиксируются оба, либо ни один.
        documentValidationOutboxService.createSuccessfulValidationEvent(payload);
        eventStatusService.saveProcessedIncomingEvent(incomingEvent);
    }

    private String validatePayload(DocumentStoredPayload payload) {
        if (payload.getOrderId() == null) {
            return "В событии document.stored отсутствует orderId";
        }
        if (payload.getUploadedByUserId() == null
                || payload.getFileName() == null || payload.getFileName().isBlank()
                || payload.getMimeType() == null || payload.getMimeType().isBlank()
                || payload.getSizeBytes() == null || payload.getSizeBytes() < 0
                || payload.getUploadedAt() == null || payload.getUploadedAt().isBlank()) {
            return "В событии document.stored отсутствуют обязательные метаданные документа";
        }
        try {
            Long.parseLong(payload.getDocumentId());
        } catch (NumberFormatException exception) {
            return "В событии document.stored поле documentId должно быть числом";
        }
        try {
            // Проверяем формат заранее, чтобы ошибка парсинга не откатила сохранённый inbox-event.
            LocalDateTime.parse(payload.getUploadedAt());
        } catch (DateTimeParseException exception) {
            return "В событии document.stored поле uploadedAt имеет неверный формат";
        }
        return null;
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
        incomingEvent.setStatus(Status.RECEIVED);
        incomingEvent.setRetryCount(0);
        incomingEvent.setReceivedAt(LocalDateTime.now());
        return incomingEventRepo.save(incomingEvent);
    }
}
