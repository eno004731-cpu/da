package document_service.services.documents;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import document_service.dto.response.UploadedDocumentResponse;
import document_service.persistence.document.DocumentEntity;
import document_service.persistence.document.DocumentRepository;
import document_service.persistence.document.DocumentEntity.Status;
import document_service.persistence.document.DocumentEntity.ValidationStatus;
import document_service.persistence.events.outbox.OutboxEventRepository;
import document_service.services.events.DocumentOutboxEventFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderDocumentsService {
    private final DocumentRepository documentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final DocumentOutboxEventFactory outboxEventFactory;
    private final DocumentFileStorage documentFileStorage;
    private final DocumentResponseMapper responseMapper;

    @Transactional
    public List<UploadedDocumentResponse> uploadDocuments(UUID orderId, Long uploadedByUserId, List<MultipartFile> documents) {
        if (documents == null || documents.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Нужно передать хотя бы один документ");
        }

        return documents.stream()
                .map(document -> storeDocument(orderId, uploadedByUserId, document))
                .map(responseMapper::toResponse)
                .toList();
    }

    private DocumentEntity storeDocument(UUID orderId, Long uploadedByUserId, MultipartFile document) {
        // Файловый storage возвращает подготовленные метаданные без обращения к БД.
        DocumentFileStorage.StoredDocumentFile storedFile = documentFileStorage.store(orderId, document);

        DocumentEntity entity = new DocumentEntity();
        entity.setOrderId(orderId);
        entity.setUploadedByUserId(uploadedByUserId);
        entity.setOriginalFileName(storedFile.originalFileName());
        entity.setStorageKey(storedFile.storageKey());
        entity.setMimeType(storedFile.mimeType());
        entity.setSizeBytes(storedFile.size());
        entity.setIsDeleted(false);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setStatus(Status.STORED);
        entity.setValidationStatus(ValidationStatus.DOCUMENT_VALIDATION_REQUESTED);

        // Сначала фиксируем документ, затем создаём связанное outbox-событие с его ID.
        DocumentEntity savedEntity = documentRepository.save(entity);
        outboxEventRepository.save(outboxEventFactory.documentStored(savedEntity));
        return savedEntity;
    }
}
