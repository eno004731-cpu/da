package document_service.services.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import document_service.dto.payload.DocumentStoredPayload;
import document_service.persistence.events.outbox.OutboxEventEntity;
import document_service.persistence.events.outbox.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentOutboxPublisherTest {

    @Test
    void publishAvailableEvents_sendsDocumentStoredEventAndMarksPublished() {
        OutboxEventRepository outboxEventRepository = mock(OutboxEventRepository.class);
        // Publisher теперь использует общий сервис статусов для outbox и incoming events.
        DocumentStatusService statusService = mock(DocumentStatusService.class);
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper();
        DocumentOutboxPublisher publisher = new DocumentOutboxPublisher(
                outboxEventRepository,
                statusService,
                kafkaTemplate,
                objectMapper
        );
        ReflectionTestUtils.setField(publisher, "documentStoredTopic", "document.stored");
        ReflectionTestUtils.setField(publisher, "documentDeletedTopic", "document.deleted");

        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        DocumentStoredPayload payload = new DocumentStoredPayload();
        payload.setEventId(eventId);
        payload.setDocumentId("10");
        payload.setOrderId(orderId);
        payload.setUploadedByUserId(7L);
        payload.setFileName("contract.pdf");
        payload.setMimeType("application/pdf");
        payload.setSizeBytes(100L);
        payload.setUploadedAt(LocalDateTime.now().toString());
        payload.setIsDeleted(false);

        OutboxEventEntity event = new OutboxEventEntity();
        event.setId(eventId);
        event.setAggregateType("DOCUMENT");
        event.setAggregateId("10");
        event.setEventType("DOCUMENT_STORED");
        event.setPayload(objectMapper.valueToTree(payload));
        event.setStatus("NEW");
        event.setRetryCount(0);
        event.setCreatedAt(LocalDateTime.now());

        when(outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc("NEW")).thenReturn(List.of(event));
        when(outboxEventRepository.findTop100ByStatusAndNextRetryAtBeforeOrderByNextRetryAtAsc(eq("FAILED"), any()))
                .thenReturn(List.of());
        when(kafkaTemplate.send(eq("document.stored"), eq(orderId.toString()), any(DocumentStoredPayload.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publishAvailableEvents();

        ArgumentCaptor<DocumentStoredPayload> payloadCaptor = ArgumentCaptor.forClass(DocumentStoredPayload.class);
        verify(statusService).markProcessing(event);
        verify(kafkaTemplate).send(eq("document.stored"), eq(orderId.toString()), payloadCaptor.capture());
        verify(statusService).markPublished(eventId);
        assertEquals("10", payloadCaptor.getValue().getDocumentId());
    }
}
