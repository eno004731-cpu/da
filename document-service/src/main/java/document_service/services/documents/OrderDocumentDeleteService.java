package document_service.services.documents;

import document_service.persistence.document.DocumentEntity;
import document_service.persistence.document.DocumentRepository;
import document_service.persistence.events.outbox.OutboxEventRepository;
import document_service.services.events.DocumentOutboxEventFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Координирует удаление физического файла, метаданных и создание outbox-события.
 */
@Service
@RequiredArgsConstructor
public class OrderDocumentDeleteService {
    private final DocumentRepository documentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final DocumentOutboxEventFactory outboxEventFactory;
    private final DocumentFileStorage documentFileStorage;

    @Transactional
    public void deleteDocument(UUID orderId, String documentId) {
        DocumentEntity document = findDocument(orderId, documentId);
        hardDeleteDocument(document);
    }

    @Transactional
    public void deleteOrderDocuments(UUID orderId) {
        // Каждый найденный документ проходит одинаковый сценарий физического и логического удаления.
        documentRepository.findAllByOrderIdOrderByCreatedAtAsc(orderId)
                .forEach(this::hardDeleteDocument);
    }

    private DocumentEntity findDocument(UUID orderId, String documentId) {
        try {
            Long id = Long.valueOf(documentId);
            return documentRepository.findByIdAndOrderId(id, orderId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Документ не найден"
                    ));
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Некорректный id документа",
                    exception
            );
        }
    }

    private void hardDeleteDocument(DocumentEntity document) {
        LocalDateTime deletedAt = LocalDateTime.now();

        // Сначала удаляем файл, чтобы при ошибке не потерять запись о его расположении в БД.
        documentFileStorage.deleteIfExists(document.getStorageKey());
        outboxEventRepository.save(outboxEventFactory.documentDeleted(document, deletedAt));
        documentRepository.delete(document);
    }
}
