package document_service.services.events.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import document_service.dto.payload.DocumentValidationResultPayload;
import document_service.persistence.document.DocumentEntity;
import document_service.persistence.document.DocumentRepository;
import document_service.persistence.document.DocumentValidationStatus;
import document_service.persistence.events.incoming.ProcessedEventEntity;
import document_service.persistence.events.incoming.ProcessedEventRepository;
import document_service.services.events.DocumentStatusService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentValidationResultListenerTest {

    @Test
    void listen_setsValidatedStatusForSuccessfulResult() {
        TestContext context = context();
        UUID orderId = UUID.randomUUID();
        DocumentEntity document = document(101L, orderId);
        DocumentValidationResultPayload payload = payload(orderId, true);
        when(context.processedEvents.existsByEventId(payload.getEventId())).thenReturn(false);
        when(context.processedEvents.save(any(ProcessedEventEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(context.documents.findByIdAndOrderId(101L, orderId))
                .thenReturn(Optional.of(document));

        context.listener.listen(record(payload));

        assertThat(document.getValidationStatus())
                .isEqualTo(DocumentValidationStatus.DOCUMENT_VALIDATED);
        assertThat(document.getValidatedAt()).isEqualTo(payload.getValidatedAt());
        verify(context.documents).save(document);
        verify(context.statusService).markProcessed(any(ProcessedEventEntity.class));
    }

    @Test
    void listen_setsRejectedStatusForFailedResult() {
        TestContext context = context();
        UUID orderId = UUID.randomUUID();
        DocumentEntity document = document(101L, orderId);
        DocumentValidationResultPayload payload = payload(orderId, false);
        when(context.processedEvents.existsByEventId(payload.getEventId())).thenReturn(false);
        when(context.processedEvents.save(any(ProcessedEventEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(context.documents.findByIdAndOrderId(101L, orderId))
                .thenReturn(Optional.of(document));

        context.listener.listen(record(payload));

        assertThat(document.getValidationStatus())
                .isEqualTo(DocumentValidationStatus.DOCUMENT_REJECTED);
        assertThat(document.isDocumentDeleted()).isTrue();
        assertThat(document.getValidatedAt()).isEqualTo(payload.getValidatedAt());
        verify(context.documents).save(document);
        verify(context.statusService).markProcessed(any(ProcessedEventEntity.class));
    }

    @Test
    void listen_doesNotRestoreDocumentAlreadyMarkedForDeletion() {
        TestContext context = context();
        UUID orderId = UUID.randomUUID();
        DocumentEntity document = document(101L, orderId);
        document.setDocumentDeleted(true);
        DocumentValidationResultPayload payload = payload(orderId, true);
        when(context.processedEvents.existsByEventId(payload.getEventId())).thenReturn(false);
        when(context.processedEvents.save(any(ProcessedEventEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(context.documents.findByIdAndOrderId(101L, orderId))
                .thenReturn(Optional.of(document));

        context.listener.listen(record(payload));

        assertThat(document.getValidationStatus())
                .isEqualTo(DocumentValidationStatus.DOCUMENT_VALIDATION_REQUESTED);
        verify(context.documents, never()).save(document);
        verify(context.statusService).markProcessed(any(ProcessedEventEntity.class));
    }

    @Test
    void listen_doesNotRestorePhysicallyDeletedDocument() {
        TestContext context = context();
        UUID orderId = UUID.randomUUID();
        DocumentEntity document = document(101L, orderId);
        document.setIsDeleted(true);
        DocumentValidationResultPayload payload = payload(orderId, true);
        when(context.processedEvents.existsByEventId(payload.getEventId())).thenReturn(false);
        when(context.processedEvents.save(any(ProcessedEventEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(context.documents.findByIdAndOrderId(101L, orderId))
                .thenReturn(Optional.of(document));

        context.listener.listen(record(payload));

        assertThat(document.getValidationStatus())
                .isEqualTo(DocumentValidationStatus.DOCUMENT_VALIDATION_REQUESTED);
        verify(context.documents, never()).save(document);
        verify(context.statusService).markProcessed(any(ProcessedEventEntity.class));
    }

    @Test
    void listen_skipsDuplicateEventWithoutUpdatingDocument() {
        TestContext context = context();
        DocumentValidationResultPayload payload = payload(UUID.randomUUID(), true);
        when(context.processedEvents.existsByEventId(payload.getEventId())).thenReturn(true);

        context.listener.listen(record(payload));

        verify(context.documents, never()).save(any(DocumentEntity.class));
        verify(context.statusService, never()).markProcessed(any(ProcessedEventEntity.class));
    }

    @Test
    void listen_savesActualKafkaMetadataInInbox() {
        TestContext context = context();
        UUID orderId = UUID.randomUUID();
        DocumentValidationResultPayload payload = payload(orderId, true);
        when(context.processedEvents.existsByEventId(payload.getEventId())).thenReturn(false);
        when(context.processedEvents.save(any(ProcessedEventEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(context.documents.findByIdAndOrderId(101L, orderId))
                .thenReturn(Optional.of(document(101L, orderId)));

        context.listener.listen(record(payload));

        ArgumentCaptor<ProcessedEventEntity> captor =
                ArgumentCaptor.forClass(ProcessedEventEntity.class);
        verify(context.processedEvents).save(captor.capture());
        ProcessedEventEntity event = captor.getValue();
        assertThat(event.getEventId()).isEqualTo(payload.getEventId());
        assertThat(event.getTopic()).isEqualTo("document.validation-result");
        assertThat(event.getPartitionNo()).isEqualTo(2);
        assertThat(event.getMessageOffset()).isEqualTo(17L);
        assertThat(event.getConsumerGroup()).isEqualTo("document-service");
        assertThat(event.getEventType()).isEqualTo("DOCUMENT_VALIDATION_RESULT");
        assertThat(event.getStatus()).isEqualTo("RECEIVED");
        assertThat(event.getPayload().get("documentId").asLong()).isEqualTo(101L);
        assertThat(event.getPayload().get("validationPassed").asBoolean()).isTrue();
    }

    private TestContext context() {
        DocumentRepository documents = mock(DocumentRepository.class);
        ProcessedEventRepository processedEvents = mock(ProcessedEventRepository.class);
        DocumentStatusService statusService = mock(DocumentStatusService.class);
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        DocumentValidationResultListener listener = new DocumentValidationResultListener(
                documents,
                processedEvents,
                statusService,
                new ObjectMapper().findAndRegisterModules(),
                validator
        );
        ReflectionTestUtils.setField(listener, "consumerGroup", "document-service");
        return new TestContext(listener, documents, processedEvents, statusService);
    }

    private ConsumerRecord<String, DocumentValidationResultPayload> record(
            DocumentValidationResultPayload payload
    ) {
        return new ConsumerRecord<>(
                "document.validation-result",
                2,
                17L,
                payload.getDocumentId().toString(),
                payload
        );
    }

    private DocumentValidationResultPayload payload(UUID orderId, boolean validationPassed) {
        DocumentValidationResultPayload payload = new DocumentValidationResultPayload();
        payload.setEventId(UUID.randomUUID());
        payload.setDocumentId(101L);
        payload.setOrderId(orderId);
        payload.setValidationPassed(validationPassed);
        payload.setValidatedAt(LocalDateTime.parse("2026-06-22T12:00:00"));
        return payload;
    }

    private DocumentEntity document(Long id, UUID orderId) {
        DocumentEntity document = new DocumentEntity();
        document.setId(id);
        document.setOrderId(orderId);
        document.setValidationStatus(
                DocumentValidationStatus.DOCUMENT_VALIDATION_REQUESTED
        );
        document.setIsDeleted(false);
        document.setDocumentDeleted(false);
        return document;
    }

    private static class TestContext {
        private final DocumentValidationResultListener listener;
        private final DocumentRepository documents;
        private final ProcessedEventRepository processedEvents;
        private final DocumentStatusService statusService;

        private TestContext(
                DocumentValidationResultListener listener,
                DocumentRepository documents,
                ProcessedEventRepository processedEvents,
                DocumentStatusService statusService
        ) {
            this.listener = listener;
            this.documents = documents;
            this.processedEvents = processedEvents;
            this.statusService = statusService;
        }
    }
}
