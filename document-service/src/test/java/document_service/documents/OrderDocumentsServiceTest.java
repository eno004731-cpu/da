package document_service.documents;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import document_service.events.DocumentOutboxEventFactory;
import document_service.events.OutboxEventEntity;
import document_service.events.OutboxEventRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
                tempDir.toString()
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
                tempDir.toString()
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

    private ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }
}
