package document_service.integration;

import document_service.documents.DocumentEntity;
import document_service.documents.DocumentRepository;
import document_service.documents.OrderDocumentsService;
import document_service.documents.UploadedDocumentResponse;
import document_service.events.OutboxEventEntity;
import document_service.events.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderDocumentsServiceIntegrationTest extends PostgresDocumentIntegrationTestBase {
    @Autowired
    private OrderDocumentsService orderDocumentsService;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Test
    void uploadDocuments_savesFileDocumentRowAndOutboxEventInOneFlow() {
        UUID orderId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "documents",
                "contract.pdf",
                "application/pdf",
                "pdf-content".getBytes()
        );

        List<UploadedDocumentResponse> response = orderDocumentsService.uploadDocuments(orderId, 77L, List.of(file));

        assertThat(response).hasSize(1);
        assertThat(response.get(0).fileName()).isEqualTo("contract.pdf");
        assertThat(response.get(0).mimeType()).isEqualTo("application/pdf");
        assertThat(response.get(0).size()).isEqualTo("pdf-content".getBytes().length);

        List<DocumentEntity> documents = documentRepository.findAllByOrderIdOrderByCreatedAtAsc(orderId);
        assertThat(documents).hasSize(1);
        DocumentEntity document = documents.get(0);
        assertThat(document.getUploadedByUserId()).isEqualTo(77L);
        assertThat(document.getOriginalFileName()).isEqualTo("contract.pdf");
        assertThat(document.getIsDeleted()).isFalse();
        assertThat(Files.exists(Path.of(documentsDir.toString(), document.getStorageKey()))).isTrue();

        List<OutboxEventEntity> events = outboxEventRepository.findAll();
        assertThat(events).hasSize(1);
        OutboxEventEntity event = events.get(0);
        assertThat(event.getEventType()).isEqualTo("DOCUMENT_STORED");
        assertThat(event.getStatus()).isEqualTo("NEW");
        assertThat(event.getAggregateId()).isEqualTo(String.valueOf(document.getId()));
        assertThat(event.getPayload().get("eventId").asText()).isEqualTo(event.getId().toString());
        assertThat(event.getPayload().get("orderId").asText()).isEqualTo(orderId.toString());
        assertThat(event.getPayload().get("documentId").asText()).isEqualTo(String.valueOf(document.getId()));
        assertThat(event.getPayload().get("uploadedByUserId").asLong()).isEqualTo(77L);
    }
}
