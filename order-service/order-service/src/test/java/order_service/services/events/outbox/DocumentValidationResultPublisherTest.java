package order_service.services.events.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import order_service.dto.payload.DocumentValidationResultPayload;
import order_service.persistence.events.outbox.OutboxEventEntity;
import order_service.persistence.events.outbox.OutboxEventRepo;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentValidationResultPublisherTest {

    @Test
    void publishAvailableEvents_sendsOnlyValidationResultEvents() {
        OutboxEventRepo repository = mock(OutboxEventRepo.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, DocumentValidationResultPayload> kafkaTemplate =
                mock(KafkaTemplate.class);
        EventStatusService statusService = mock(EventStatusService.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        DocumentValidationResultPublisher publisher = new DocumentValidationResultPublisher(
                repository,
                objectMapper,
                kafkaTemplate,
                statusService
        );
        ReflectionTestUtils.setField(publisher, "topic", "document.validation-result");

        OutboxEventEntity event = validationEvent(objectMapper);
        when(repository.findTop100ByStatusAndEventTypeOrderByCreatedAtAsc(
                "NEW",
                "DOCUMENT_VALIDATION_RESULT"
        )).thenReturn(List.of(event));
        when(repository.findTop100ByStatusAndEventTypeAndNextRetryAtBeforeOrderByNextRetryAtAsc(
                eq("FAILED"),
                eq("DOCUMENT_VALIDATION_RESULT"),
                any(LocalDateTime.class)
        )).thenReturn(List.of());
        when(kafkaTemplate.send(
                eq("document.validation-result"),
                eq("101"),
                any(DocumentValidationResultPayload.class)
        )).thenReturn(CompletableFuture.completedFuture(null));

        publisher.publishAvailableEvents();

        verify(kafkaTemplate).send(
                eq("document.validation-result"),
                eq("101"),
                any(DocumentValidationResultPayload.class)
        );
        verify(statusService).savePublishedEvent(event);
    }

    @Test
    void publishAvailableEvents_marksEventFailedWhenKafkaRejectsSend() {
        OutboxEventRepo repository = mock(OutboxEventRepo.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, DocumentValidationResultPayload> kafkaTemplate =
                mock(KafkaTemplate.class);
        EventStatusService statusService = mock(EventStatusService.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        DocumentValidationResultPublisher publisher = new DocumentValidationResultPublisher(
                repository,
                objectMapper,
                kafkaTemplate,
                statusService
        );
        ReflectionTestUtils.setField(publisher, "topic", "document.validation-result");

        OutboxEventEntity event = validationEvent(objectMapper);
        when(repository.findTop100ByStatusAndEventTypeOrderByCreatedAtAsc(
                "NEW",
                "DOCUMENT_VALIDATION_RESULT"
        )).thenReturn(List.of(event));
        when(repository.findTop100ByStatusAndEventTypeAndNextRetryAtBeforeOrderByNextRetryAtAsc(
                eq("FAILED"),
                eq("DOCUMENT_VALIDATION_RESULT"),
                any(LocalDateTime.class)
        )).thenReturn(List.of());
        CompletableFuture<org.springframework.kafka.support.SendResult<String, DocumentValidationResultPayload>>
                failedSend = new CompletableFuture<>();
        failedSend.completeExceptionally(new IllegalStateException("Kafka unavailable"));
        when(kafkaTemplate.send(
                eq("document.validation-result"),
                eq("101"),
                any(DocumentValidationResultPayload.class)
        )).thenReturn(failedSend);

        publisher.publishAvailableEvents();

        verify(statusService).saveFailedEvent(
                eq(event),
                org.mockito.ArgumentMatchers.contains("Kafka unavailable")
        );
    }

    private OutboxEventEntity validationEvent(ObjectMapper objectMapper) {
        UUID eventId = UUID.randomUUID();
        DocumentValidationResultPayload payload = new DocumentValidationResultPayload();
        payload.setEventId(eventId);
        payload.setDocumentId(101L);
        payload.setOrderId(UUID.randomUUID());
        payload.setValidationPassed(true);
        payload.setValidatedAt(LocalDateTime.parse("2026-06-22T12:00:00"));

        OutboxEventEntity event = new OutboxEventEntity();
        event.setId(eventId);
        event.setAggregateId(payload.getOrderId());
        event.setEventType("DOCUMENT_VALIDATION_RESULT");
        event.setStatus("NEW");
        event.setRetryCount(0);
        event.setCreatedAt(LocalDateTime.parse("2026-06-22T12:00:00"));
        event.setPayload(objectMapper.valueToTree(payload));
        return event;
    }
}
