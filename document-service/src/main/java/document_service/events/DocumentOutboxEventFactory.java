package document_service.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import document_service.documents.DocumentEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DocumentOutboxEventFactory {
    private final ObjectMapper objectMapper;

    public OutboxEventEntity documentStored(DocumentEntity document) {
        UUID eventId = UUID.randomUUID();
        DocumentStoredPayload payload = new DocumentStoredPayload();
        payload.setEventId(eventId);
        payload.setDocumentId(String.valueOf(document.getId()));
        payload.setOrderId(document.getOrderId());
        payload.setUploadedByUserId(document.getUploadedByUserId());
        payload.setFileName(document.getOriginalFileName());
        payload.setMimeType(document.getMimeType());
        payload.setSizeBytes(document.getSizeBytes());
        payload.setUploadedAt(document.getCreatedAt().toString());
        payload.setIsDeleted(Boolean.TRUE.equals(document.getIsDeleted()));

        OutboxEventEntity event = new OutboxEventEntity();
        event.setId(eventId);
        event.setAggregateType("DOCUMENT");
        event.setAggregateId(String.valueOf(document.getId()));
        event.setEventType("DOCUMENT_STORED");
        event.setPayload(objectMapper.valueToTree(payload));
        event.setStatus("NEW");
        event.setRetryCount(0);
        event.setCreatedAt(LocalDateTime.now());
        return event;
    }

    public OutboxEventEntity documentDeleted(DocumentEntity document, LocalDateTime deletedAt) {
        UUID eventId = UUID.randomUUID();
        DocumentDeletedPayload payload = new DocumentDeletedPayload();
        payload.setEventId(eventId);
        payload.setDocumentId(String.valueOf(document.getId()));
        payload.setOrderId(document.getOrderId());
        payload.setDeletedAt(deletedAt.toString());
        payload.setIsDeleted(true);

        OutboxEventEntity event = new OutboxEventEntity();
        event.setId(eventId);
        event.setAggregateType("DOCUMENT");
        event.setAggregateId(String.valueOf(document.getId()));
        event.setEventType("DOCUMENT_DELETED");
        event.setPayload(objectMapper.valueToTree(payload));
        event.setStatus("NEW");
        event.setRetryCount(0);
        event.setCreatedAt(LocalDateTime.now());
        return event;
    }
}
