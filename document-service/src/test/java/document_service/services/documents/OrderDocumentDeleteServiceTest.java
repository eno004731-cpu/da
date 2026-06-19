package document_service.services.documents;

import com.fasterxml.jackson.databind.ObjectMapper;
import document_service.persistence.document.DocumentEntity;
import document_service.persistence.document.DocumentRepository;
import document_service.persistence.events.outbox.OutboxEventEntity;
import document_service.persistence.events.outbox.OutboxEventRepository;
import document_service.services.events.DocumentOutboxEventFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderDocumentDeleteServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void deleteDocument_deletesFileMetadataAndCreatesOutboxEvent() throws Exception {
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        OutboxEventRepository outboxEventRepository = mock(OutboxEventRepository.class);
        OrderDocumentDeleteService service = createService(documentRepository, outboxEventRepository);
        UUID orderId = UUID.randomUUID();
        Path storedFile = tempDir.resolve("stored.pdf");
        Files.writeString(storedFile, "content");
        DocumentEntity entity = createDocument(orderId, 10L, "stored.pdf");

        when(documentRepository.findByIdAndOrderId(10L, orderId))
                .thenReturn(java.util.Optional.of(entity));

        service.deleteDocument(orderId, "10");

        assertFalse(Files.exists(storedFile));
        verify(documentRepository).delete(entity);
        verify(outboxEventRepository).save(any(OutboxEventEntity.class));
    }

    @Test
    void deleteDocument_deletesMetadataWhenFileAlreadyMissing() {
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        OutboxEventRepository outboxEventRepository = mock(OutboxEventRepository.class);
        OrderDocumentDeleteService service = createService(documentRepository, outboxEventRepository);
        UUID orderId = UUID.randomUUID();
        DocumentEntity entity = createDocument(orderId, 11L, "missing.pdf");

        when(documentRepository.findByIdAndOrderId(11L, orderId))
                .thenReturn(java.util.Optional.of(entity));

        service.deleteDocument(orderId, "11");

        verify(documentRepository).delete(entity);
        verify(outboxEventRepository).save(any(OutboxEventEntity.class));
    }

    private OrderDocumentDeleteService createService(
            DocumentRepository documentRepository,
            OutboxEventRepository outboxEventRepository
    ) {
        // В тесте используется настоящее временное файловое хранилище.
        return new OrderDocumentDeleteService(
                documentRepository,
                outboxEventRepository,
                new DocumentOutboxEventFactory(new ObjectMapper().findAndRegisterModules()),
                new DocumentFileStorage(tempDir.toString())
        );
    }

    private DocumentEntity createDocument(UUID orderId, Long documentId, String storageKey) {
        DocumentEntity entity = new DocumentEntity();
        entity.setId(documentId);
        entity.setOrderId(orderId);
        entity.setStorageKey(storageKey);
        entity.setOriginalFileName(storageKey);
        entity.setMimeType("application/pdf");
        entity.setSizeBytes(7L);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setIsDeleted(false);
        return entity;
    }
}
