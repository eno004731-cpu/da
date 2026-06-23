package document_service.integration;

import document_service.dto.response.UploadedDocumentResponse;
import document_service.persistence.document.DocumentEntity;
import document_service.persistence.document.DocumentRepository;
import document_service.persistence.document.DocumentValidationStatus;
import document_service.persistence.events.outbox.OutboxEventEntity;
import document_service.persistence.events.outbox.OutboxEventRepository;
import document_service.services.documents.store.OrderDocumentsService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderDocumentsServiceIntegrationTest extends PostgresDocumentIntegrationTestBase {
    @Autowired
    private OrderDocumentsService orderDocumentsService;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Test
    void uploadDocuments_savesFileDocumentRowAndOutboxEventInOneFlow() throws Exception {
        UUID orderId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "documents",
                "contract.pdf",
                "application/pdf",
                "pdf-content".getBytes()
        );

        List<UploadedDocumentResponse> response = orderDocumentsService.uploadDocuments(orderId, 77L, List.of(file));

        assertThat(response).hasSize(1);
        // DTO является Lombok-классом, поэтому контракт проверяем через JavaBean-getter'ы.
        assertThat(response.get(0).getFileName()).isEqualTo("contract.pdf");
        assertThat(response.get(0).getMimeType()).isEqualTo("application/pdf");
        assertThat(response.get(0).getSize()).isEqualTo("pdf-content".getBytes().length);

        List<DocumentEntity> documents = documentRepository.findAllByOrderIdOrderByCreatedAtAsc(orderId);
        assertThat(documents).hasSize(1);
        DocumentEntity document = documents.get(0);
        // Ответ и persistence-запись должны ссылаться на один и тот же документ.
        assertThat(response.get(0).getId()).isEqualTo(document.getId().toString());
        assertThat(document.getUploadedByUserId()).isEqualTo(77L);
        assertThat(document.getOriginalFileName()).isEqualTo("contract.pdf");
        assertThat(document.getIsDeleted()).isFalse();
        assertThat(document.isDocumentDeleted()).isFalse();
        assertThat(document.getValidationStatus())
                .isEqualTo(DocumentValidationStatus.DOCUMENT_VALIDATION_REQUESTED);

        Path storedFile = documentsDir.resolve(document.getStorageKey());
        assertThat(storedFile).exists();
        // Проверяем содержимое, чтобы тест не прошёл при создании пустого файла.
        assertThat(Files.readString(storedFile)).isEqualTo("pdf-content");

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

    @Test
    void uploadDocuments_rejectsEmptyListWithoutPersistingAnything() {
        UUID orderId = UUID.randomUUID();

        // Ошибка является частью контракта: пустой запрос не должен молча считаться успешным.
        assertThatThrownBy(() -> orderDocumentsService.uploadDocuments(orderId, 77L, List.of()))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("Нужно передать хотя бы один документ");

        // После отказа не должно оставаться ни metadata, ни outbox-событий.
        assertThat(documentRepository.count()).isZero();
        assertThat(outboxEventRepository.count()).isZero();
    }
}
