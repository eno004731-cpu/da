package document_service.services.documents;

import document_service.dto.response.UploadedDocumentResponse;
import document_service.persistence.document.DocumentEntity;
import document_service.persistence.document.DocumentRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderDocumentsQueryServiceTest {

    @Test
    void listDocuments_mapsRepositoryEntities() {
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        OrderDocumentsQueryService service = new OrderDocumentsQueryService(
                documentRepository,
                new DocumentResponseMapper()
        );
        UUID orderId = UUID.randomUUID();

        // Entity имитирует документ, прочитанный из базы данных.
        DocumentEntity entity = new DocumentEntity();
        entity.setId(10L);
        entity.setOrderId(orderId);
        entity.setOriginalFileName("act.pdf");
        entity.setMimeType("application/pdf");
        entity.setSizeBytes(123L);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setIsDeleted(false);

        when(documentRepository.findAllByOrderIdOrderByCreatedAtAsc(orderId))
                .thenReturn(List.of(entity));

        List<UploadedDocumentResponse> response = service.listDocuments(orderId);

        assertEquals(1, response.size());
        assertEquals("10", response.get(0).id());
        assertEquals("act.pdf", response.get(0).fileName());
    }
}
