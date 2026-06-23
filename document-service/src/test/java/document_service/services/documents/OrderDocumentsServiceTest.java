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
import document_service.services.documents.store.DocumentFileStorage;
import document_service.services.documents.store.OrderDocumentsService;
import document_service.services.events.DocumentOutboxEventFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
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
                new DocumentFileStorage(tempDir.toString()),
                new DocumentResponseMapper()
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
        // Проверяем внешний DTO, а не только аргумент, переданный в repository.
        assertEquals("contract.pdf", response.get(0).getFileName());

        ArgumentCaptor<OutboxEventEntity> eventCaptor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(outboxEventRepository).save(eventCaptor.capture());
        assertEquals("DOCUMENT_STORED", eventCaptor.getValue().getEventType());
        assertEquals("NEW", eventCaptor.getValue().getStatus());
    }

    @Test
    void uploadDocuments_rejectsEmptyListBeforeStorageAndRepositories() {
        OrderDocumentsService service = new OrderDocumentsService(
                documentRepository,
                outboxEventRepository,
                new DocumentOutboxEventFactory(objectMapper()),
                new DocumentFileStorage(tempDir.toString()),
                new DocumentResponseMapper()
        );

        // Проверяем само исключение, поэтому его нельзя случайно проглотить внутри теста.
        assertThatThrownBy(() -> service.uploadDocuments(UUID.randomUUID(), 7L, List.of()))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("Нужно передать хотя бы один документ");

        // Валидация должна сработать до любых операций с БД.
        verifyNoInteractions(documentRepository, outboxEventRepository);
    }

    private ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }
}
