package document_service.services.documents;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import document_service.dto.response.UploadedDocumentResponse;
import document_service.persistence.document.DocumentEntity;
import document_service.persistence.document.DocumentRepository;
import document_service.persistence.events.outbox.OutboxEventEntity;
import document_service.persistence.events.outbox.OutboxEventRepository;
import document_service.services.events.DocumentOutboxEventFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderDocumentsServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @TempDir
    Path tempDir;

    @Test
    void uploadDocuments_savesFileAndMetadata() throws Exception {
        OrderDocumentsService service = new OrderDocumentsService(
                documentRepository,
                outboxEventRepository,
                new DocumentOutboxEventFactory(objectMapper()),
                tempDir
        );
        UUID orderId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("documents", "contract.pdf", "application/pdf", "hello".getBytes());

        when(documentRepository.save(any(DocumentEntity.class))).thenAnswer(invocation -> {
            DocumentEntity entity = invocation.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        List<UploadedDocumentResponse> response = service.uploadDocuments(orderId, 7L, List.of(file));

        ArgumentCaptor<DocumentEntity> captor = ArgumentCaptor.forClass(DocumentEntity.class);
        verify(documentRepository).save(captor.capture());
        DocumentEntity savedEntity = captor.getValue();
        assertEquals(orderId, savedEntity.getOrderId());
        assertEquals(7L, savedEntity.getUploadedByUserId());
        assertEquals("contract.pdf", savedEntity.getOriginalFileName());
        assertTrue(Files.exists(tempDir.resolve(savedEntity.getStorageKey())));
        assertEquals(1, response.size());
        assertEquals("contract.pdf", response.get(0).fileName());

        ArgumentCaptor<OutboxEventEntity> eventCaptor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(outboxEventRepository).save(eventCaptor.capture());
        assertEquals("DOCUMENT_STORED", eventCaptor.getValue().getEventType());
        assertEquals("NEW", eventCaptor.getValue().getStatus());
    }

    @Test
    void listDocuments_mapsRepositoryEntities() {
        OrderDocumentsService service = new OrderDocumentsService(
                documentRepository,
                outboxEventRepository,
                new DocumentOutboxEventFactory(objectMapper()),
                tempDir
        );
        UUID orderId = UUID.randomUUID();
        DocumentEntity entity = new DocumentEntity();
        entity.setId(10L);
        entity.setOrderId(orderId);
        entity.setOriginalFileName("act.pdf");
        entity.setMimeType("application/pdf");
        entity.setSizeBytes(123L);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setIsDeleted(false);

        when(documentRepository.findAllByOrderIdOrderByCreatedAtAsc(orderId)).thenReturn(List.of(entity));

        List<UploadedDocumentResponse> response = service.listDocuments(orderId);

        assertEquals(1, response.size());
        assertEquals("10", response.get(0).id());
        assertEquals("act.pdf", response.get(0).fileName());
    }

    @Test
    void deleteDocument_deletesFileAndMetadataAndCreatesOutboxEvent() throws Exception {
        OrderDocumentsService service = new OrderDocumentsService(
                documentRepository,
                outboxEventRepository,
                new DocumentOutboxEventFactory(objectMapper()),
                tempDir
        );
        UUID orderId = UUID.randomUUID();
        Path storedFile = tempDir.resolve("stored.pdf");
        Files.writeString(storedFile, "content");

        DocumentEntity entity = new DocumentEntity();
        entity.setId(10L);
        entity.setOrderId(orderId);
        entity.setStorageKey("stored.pdf");
        entity.setOriginalFileName("stored.pdf");
        entity.setMimeType("application/pdf");
        entity.setSizeBytes(7L);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setIsDeleted(false);

        when(documentRepository.findByIdAndOrderId(10L, orderId)).thenReturn(java.util.Optional.of(entity));

        service.deleteDocument(orderId, "10");

        assertFalse(Files.exists(storedFile));
        verify(documentRepository).delete(entity);
        ArgumentCaptor<OutboxEventEntity> eventCaptor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(outboxEventRepository).save(eventCaptor.capture());
        assertEquals("DOCUMENT_DELETED", eventCaptor.getValue().getEventType());
    }

    @Test
    void deleteDocument_deletesMetadataWhenFileAlreadyMissing() {
        OrderDocumentsService service = new OrderDocumentsService(
                documentRepository,
                outboxEventRepository,
                new DocumentOutboxEventFactory(objectMapper()),
                tempDir
        );
        UUID orderId = UUID.randomUUID();
        DocumentEntity entity = new DocumentEntity();
        entity.setId(11L);
        entity.setOrderId(orderId);
        entity.setStorageKey("missing.pdf");
        entity.setOriginalFileName("missing.pdf");
        entity.setMimeType("application/pdf");
        entity.setSizeBytes(7L);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setIsDeleted(false);

        when(documentRepository.findByIdAndOrderId(11L, orderId)).thenReturn(java.util.Optional.of(entity));

        service.deleteDocument(orderId, "11");

        verify(documentRepository).delete(entity);
        verify(outboxEventRepository).save(any(OutboxEventEntity.class));
    }

    private ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }
}
