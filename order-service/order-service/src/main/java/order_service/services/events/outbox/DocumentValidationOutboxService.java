package order_service.services.events.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import order_service.dto.payload.DocumentStoredPayload;
import order_service.dto.payload.DocumentValidationResultPayload;
import order_service.persistence.events.outbox.OutboxEventEntity;
import order_service.persistence.events.outbox.OutboxEventRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Создаёт outbox-событие после успешной проверки документа.
 */
@Service
@RequiredArgsConstructor
public class DocumentValidationOutboxService {
    public static final String EVENT_TYPE = "DOCUMENT_VALIDATION_RESULT";

    private final OutboxEventRepo outboxEventRepo;
    private final ObjectMapper objectMapper;

    @Transactional
    public OutboxEventEntity createSuccessfulValidationEvent(DocumentStoredPayload storedDocument) {
        Long documentId = parseDocumentId(storedDocument.getDocumentId());

        OutboxEventEntity event = new OutboxEventEntity();
        event.setId(UUID.randomUUID());
        event.setAggregateId(storedDocument.getOrderId());
        event.setEventType(EVENT_TYPE);
        event.setStatus("NEW");
        event.setRetryCount(0);
        event.setCreatedAt(LocalDateTime.now());

        DocumentValidationResultPayload payload = new DocumentValidationResultPayload();
        payload.setEventId(event.getId());
        payload.setDocumentId(documentId);
        payload.setOrderId(storedDocument.getOrderId());
        payload.setValidationPassed(true);
        payload.setValidatedAt(LocalDateTime.now());

        // Ошибка сериализации должна откатить общую транзакцию inbox + outbox.
        event.setPayload(objectMapper.valueToTree(payload));
        return outboxEventRepo.save(event);
    }

    private Long parseDocumentId(String rawDocumentId) {
        try {
            return Long.valueOf(rawDocumentId);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "documentId должен быть числовым идентификатором",
                    exception
            );
        }
    }
}
