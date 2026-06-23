package document_service.services.events.validation;

import document_service.persistence.document.DocumentEntity;
import document_service.persistence.document.DocumentRepository;
import document_service.persistence.document.DocumentValidationStatus;
import document_service.persistence.events.outbox.OutboxEventEntity;
import document_service.persistence.events.outbox.OutboxEventRepository;
import document_service.services.events.DocumentOutboxEventFactory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class DocumentValidationRetryServiceTest {

    @Test
    void createRetryEvents_createsNewOutboxEventAndMovesRetryTimestamp() {
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        OutboxEventRepository outboxEventRepository = mock(OutboxEventRepository.class);
        DocumentOutboxEventFactory eventFactory = mock(DocumentOutboxEventFactory.class);
        DocumentValidationRetryService service = new DocumentValidationRetryService(
                documentRepository,
                outboxEventRepository,
                eventFactory
        );
        ReflectionTestUtils.setField(service, "retryAfterSeconds", 30L);

        DocumentEntity document = document();
        LocalDateTime previousRequestTime =
                LocalDateTime.parse("2026-06-22T11:00:00");
        document.setValidationRequestedAt(previousRequestTime);
        OutboxEventEntity retryEvent = new OutboxEventEntity();
        retryEvent.setId(UUID.randomUUID());

        when(documentRepository
                .findTop100ByValidationStatusAndIsDeletedFalseAndIsDocumentDeletedFalseAndValidationRequestedAtLessThanEqualOrderByValidationRequestedAtAsc(
                        eq(DocumentValidationStatus.DOCUMENT_VALIDATION_REQUESTED),
                        any(LocalDateTime.class)
                ))
                .thenReturn(List.of(document));
        when(outboxEventRepository.existsByAggregateIdAndEventTypeAndStatusIn(
                document.getId().toString(),
                DocumentOutboxEventFactory.DOCUMENT_STORED_EVENT_TYPE,
                List.of("NEW", "PROCESSING", "FAILED")
        )).thenReturn(false);
        when(eventFactory.documentStored(document)).thenReturn(retryEvent);

        service.createRetryEvents();

        verify(outboxEventRepository).save(retryEvent);
        verify(documentRepository).save(document);
        assertThat(document.getValidationRequestedAt()).isAfter(previousRequestTime);

        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(documentRepository)
                .findTop100ByValidationStatusAndIsDeletedFalseAndIsDocumentDeletedFalseAndValidationRequestedAtLessThanEqualOrderByValidationRequestedAtAsc(
                        eq(DocumentValidationStatus.DOCUMENT_VALIDATION_REQUESTED),
                        cutoff.capture()
                );
        assertThat(cutoff.getValue()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    private DocumentEntity document() {
        DocumentEntity document = new DocumentEntity();
        document.setId(101L);
        document.setOrderId(UUID.randomUUID());
        document.setIsDeleted(false);
        document.setDocumentDeleted(false);
        document.setValidationStatus(
                DocumentValidationStatus.DOCUMENT_VALIDATION_REQUESTED
        );
        return document;
    }

    @Test
    void createRetryEvents_skipsDocumentWithActiveOutboxEvent() {
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        OutboxEventRepository outboxEventRepository = mock(OutboxEventRepository.class);
        DocumentOutboxEventFactory eventFactory = mock(DocumentOutboxEventFactory.class);
        DocumentValidationRetryService service = new DocumentValidationRetryService(
                documentRepository,
                outboxEventRepository,
                eventFactory
        );
        ReflectionTestUtils.setField(service, "retryAfterSeconds", 30L);
        DocumentEntity document = document();

        when(documentRepository
                .findTop100ByValidationStatusAndIsDeletedFalseAndIsDocumentDeletedFalseAndValidationRequestedAtLessThanEqualOrderByValidationRequestedAtAsc(
                        eq(DocumentValidationStatus.DOCUMENT_VALIDATION_REQUESTED),
                        any(LocalDateTime.class)
                ))
                .thenReturn(List.of(document));
        when(outboxEventRepository.existsByAggregateIdAndEventTypeAndStatusIn(
                document.getId().toString(),
                DocumentOutboxEventFactory.DOCUMENT_STORED_EVENT_TYPE,
                List.of("NEW", "PROCESSING", "FAILED")
        )).thenReturn(true);

        service.createRetryEvents();

        verify(eventFactory, never()).documentStored(document);
        verify(outboxEventRepository, never()).save(any(OutboxEventEntity.class));
        verify(documentRepository, never()).save(document);
    }
}
