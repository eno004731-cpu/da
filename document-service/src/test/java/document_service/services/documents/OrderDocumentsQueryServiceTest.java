package document_service.services.documents;

import document_service.dto.response.UploadedDocumentResponse;
import document_service.persistence.document.DocumentEntity;
import document_service.persistence.document.DocumentRepository;
import document_service.persistence.document.DocumentValidationStatus;
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
        Long userId = 77L;

        // Entity имитирует документ, прочитанный из базы данных.
        DocumentEntity entity = new DocumentEntity();
        entity.setId(10L);
        entity.setOrderId(orderId);
        entity.setOriginalFileName("act.pdf");
        entity.setMimeType("application/pdf");
        entity.setSizeBytes(123L);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setIsDeleted(false);
        entity.setValidationStatus(DocumentValidationStatus.DOCUMENT_VALIDATION_REQUESTED);

        when(documentRepository.findAllByOrderIdAndUploadedByUserIdOrderByCreatedAtAsc(orderId, userId))
                .thenReturn(List.of(entity));

        List<UploadedDocumentResponse> response = service.listDocuments(orderId, userId);

        assertEquals(1, response.size());
        // DTO является Lombok-классом, поэтому результат читается через JavaBean-getter'ы.
        assertEquals("10", response.get(0).getId());
        assertEquals("act.pdf", response.get(0).getFileName());
        assertEquals(
                "DOCUMENT_VALIDATION_REQUESTED",
                response.get(0).getValidationStatus()
        );
    }
}
