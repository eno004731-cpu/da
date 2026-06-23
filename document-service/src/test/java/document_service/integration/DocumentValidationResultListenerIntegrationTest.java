package document_service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import document_service.dto.payload.DocumentValidationResultPayload;
import document_service.persistence.document.DocumentEntity;
import document_service.persistence.document.DocumentRepository;
import document_service.persistence.document.DocumentValidationStatus;
import document_service.persistence.events.incoming.ProcessedEventEntity;
import document_service.persistence.events.incoming.ProcessedEventRepository;
import document_service.persistence.events.outbox.OutboxEventEntity;
import document_service.persistence.events.outbox.OutboxEventRepository;
import document_service.services.events.DocumentStatusService;
import document_service.services.events.DocumentOutboxEventFactory;
import document_service.services.events.validation.DocumentValidationResultListener;
import document_service.services.events.validation.DocumentValidationRetryService;
import jakarta.validation.Validation;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentValidationResultListenerIntegrationTest extends PostgresDocumentIntegrationTestBase {
    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private DocumentStatusService documentStatusService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private DocumentOutboxEventFactory documentOutboxEventFactory;

    @Test
    void listener_updatesDocumentAndPersistsInboxOnlyOnce() {
        UUID orderId = UUID.randomUUID();
        DocumentEntity document = documentRepository.save(document(orderId));
        DocumentValidationResultPayload payload = payload(document.getId(), orderId);
        DocumentValidationResultListener listener = new DocumentValidationResultListener(
                documentRepository,
                processedEventRepository,
                documentStatusService,
                objectMapper,
                Validation.buildDefaultValidatorFactory().getValidator()
        );
        ReflectionTestUtils.setField(listener, "consumerGroup", "document-service-integration-test");

        listener.listen(record(payload, 17L));
        listener.listen(record(payload, 18L));

        DocumentEntity updatedDocument = documentRepository.findById(document.getId()).orElseThrow();
        assertThat(updatedDocument.getValidationStatus())
                .isEqualTo(DocumentValidationStatus.DOCUMENT_VALIDATED);
        assertThat(updatedDocument.getValidatedAt()).isEqualTo(payload.getValidatedAt());

        assertThat(processedEventRepository.findAll()).hasSize(1);
        ProcessedEventEntity inboxEvent =
                processedEventRepository.findByEventId(payload.getEventId()).orElseThrow();
        assertThat(inboxEvent.getStatus()).isEqualTo("PROCESSED");
        assertThat(inboxEvent.getEventType()).isEqualTo("DOCUMENT_VALIDATION_RESULT");
        assertThat(inboxEvent.getPayload().get("documentId").asLong())
                .isEqualTo(document.getId());
        assertThat(inboxEvent.getPayload().get("validationPassed").asBoolean()).isTrue();
    }

    @Test
    void listener_marksRejectedDocumentForPhysicalDeletion() {
        UUID orderId = UUID.randomUUID();
        DocumentEntity document = documentRepository.save(document(orderId));
        DocumentValidationResultPayload payload = payload(document.getId(), orderId);
        payload.setValidationPassed(false);
        DocumentValidationResultListener listener = listener();

        listener.listen(record(payload, 22L));

        DocumentEntity rejectedDocument =
                documentRepository.findById(document.getId()).orElseThrow();
        assertThat(rejectedDocument.getValidationStatus())
                .isEqualTo(DocumentValidationStatus.DOCUMENT_REJECTED);
        assertThat(rejectedDocument.isDocumentDeleted()).isTrue();
        assertThat(rejectedDocument.getIsDeleted()).isFalse();
        assertThat(rejectedDocument.getValidatedAt()).isEqualTo(payload.getValidatedAt());
    }

    @Test
    void retryService_createsEventOnlyForPendingDocumentThatIsNotDeleted() {
        LocalDateTime oldRequestTime = LocalDateTime.now().minusMinutes(5);

        DocumentEntity pendingDocument = documentRepository.save(document(UUID.randomUUID()));
        pendingDocument.setValidationRequestedAt(oldRequestTime);
        documentRepository.save(pendingDocument);

        DocumentEntity deletedDocument = document(UUID.randomUUID());
        deletedDocument.setStorageKey(deletedDocument.getOrderId() + "/deleted.pdf");
        deletedDocument.setValidationRequestedAt(oldRequestTime);
        deletedDocument.setIsDeleted(true);
        documentRepository.save(deletedDocument);

        DocumentEntity validatedDocument = document(UUID.randomUUID());
        validatedDocument.setStorageKey(validatedDocument.getOrderId() + "/validated.pdf");
        validatedDocument.setValidationRequestedAt(oldRequestTime);
        validatedDocument.setValidationStatus(
                DocumentValidationStatus.DOCUMENT_VALIDATED
        );
        documentRepository.save(validatedDocument);

        DocumentValidationRetryService retryService = new DocumentValidationRetryService(
                documentRepository,
                outboxEventRepository,
                documentOutboxEventFactory
        );
        ReflectionTestUtils.setField(retryService, "retryAfterSeconds", 30L);

        retryService.createRetryEvents();

        assertThat(outboxEventRepository.findAll()).hasSize(1);
        OutboxEventEntity retryEvent = outboxEventRepository.findAll().get(0);
        assertThat(retryEvent.getEventType()).isEqualTo("DOCUMENT_STORED");
        assertThat(retryEvent.getAggregateId())
                .isEqualTo(pendingDocument.getId().toString());
        assertThat(retryEvent.getPayload().get("eventId").asText())
                .isEqualTo(retryEvent.getId().toString());

        DocumentEntity updatedPending =
                documentRepository.findById(pendingDocument.getId()).orElseThrow();
        assertThat(updatedPending.getValidationRequestedAt()).isAfter(oldRequestTime);
    }

    private DocumentValidationResultListener listener() {
        DocumentValidationResultListener listener = new DocumentValidationResultListener(
                documentRepository,
                processedEventRepository,
                documentStatusService,
                objectMapper,
                Validation.buildDefaultValidatorFactory().getValidator()
        );
        ReflectionTestUtils.setField(listener, "consumerGroup", "document-service-integration-test");
        return listener;
    }

    private ConsumerRecord<String, DocumentValidationResultPayload> record(
            DocumentValidationResultPayload payload,
            long offset
    ) {
        return new ConsumerRecord<>(
                "document.validation-result",
                1,
                offset,
                payload.getDocumentId().toString(),
                payload
        );
    }

    private DocumentValidationResultPayload payload(Long documentId, UUID orderId) {
        DocumentValidationResultPayload payload = new DocumentValidationResultPayload();
        payload.setEventId(UUID.randomUUID());
        payload.setDocumentId(documentId);
        payload.setOrderId(orderId);
        payload.setValidationPassed(true);
        payload.setValidatedAt(LocalDateTime.parse("2026-06-22T12:00:00"));
        return payload;
    }

    private DocumentEntity document(UUID orderId) {
        DocumentEntity document = new DocumentEntity();
        document.setOrderId(orderId);
        document.setUploadedByUserId(7L);
        document.setOriginalFileName("contract.pdf");
        document.setStorageKey(orderId + "/contract.pdf");
        document.setMimeType("application/pdf");
        document.setSizeBytes(2048L);
        document.setValidationStatus(
                DocumentValidationStatus.DOCUMENT_VALIDATION_REQUESTED
        );
        document.setIsDeleted(false);
        document.setDocumentDeleted(false);
        document.setCreatedAt(LocalDateTime.parse("2026-06-22T11:00:00"));
        document.setValidationRequestedAt(LocalDateTime.parse("2026-06-22T11:00:00"));
        return document;
    }
}
