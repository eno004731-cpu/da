package document_service.documents;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import document_service.events.DocumentOutboxEventFactory;
import document_service.events.OutboxEventRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class OrderDocumentsService {
    private final DocumentRepository documentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final DocumentOutboxEventFactory outboxEventFactory;
    private final Path documentsDir;

    public OrderDocumentsService(
            DocumentRepository documentRepository,
            OutboxEventRepository outboxEventRepository,
            DocumentOutboxEventFactory outboxEventFactory,
            @Value("${app.storage.documents-dir}") String documentsDir
    ) {
        this.documentRepository = documentRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.outboxEventFactory = outboxEventFactory;
        this.documentsDir = Path.of(documentsDir).toAbsolutePath().normalize();
    }

    public List<UploadedDocumentResponse> listDocuments(UUID orderId) {
        return documentRepository.findAllByOrderIdOrderByCreatedAtAsc(orderId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public List<UploadedDocumentResponse> uploadDocuments(UUID orderId, Long uploadedByUserId, List<MultipartFile> documents) {
        if (documents == null || documents.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Нужно передать хотя бы один документ");
        }

        return documents.stream()
                .map(document -> storeDocument(orderId, uploadedByUserId, document))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void deleteDocument(UUID orderId, String documentId) {
        DocumentEntity document = findDocument(orderId, documentId);
        hardDeleteDocument(document);
    }

    @Transactional
    public void deleteOrderDocuments(UUID orderId) {
        documentRepository.findAllByOrderIdOrderByCreatedAtAsc(orderId)
                .forEach(this::hardDeleteDocument);
    }

    private DocumentEntity findDocument(UUID orderId, String documentId) {
        try {
            Long id = Long.valueOf(documentId);
            return documentRepository.findByIdAndOrderId(id, orderId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Документ не найден"));
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Некорректный id документа", e);
        }
    }

    private void hardDeleteDocument(DocumentEntity document) {
        LocalDateTime deletedAt = LocalDateTime.now();
        deleteFileIfExists(document);
        outboxEventRepository.save(outboxEventFactory.documentDeleted(document, deletedAt));
        documentRepository.delete(document);
    }

    private void deleteFileIfExists(DocumentEntity document) {
        Path storagePath = documentsDir.resolve(document.getStorageKey()).normalize();
        try {
            Files.deleteIfExists(storagePath);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Не удалось удалить документ", e);
        }
    }

    private DocumentEntity storeDocument(UUID orderId, Long uploadedByUserId, MultipartFile document) {
        if (document.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Пустые документы загружать нельзя");
        }

        String originalFileName = resolveOriginalFileName(document);
        String storageKey = orderId + "/" + UUID.randomUUID() + "-" + originalFileName;
        Path storagePath = documentsDir.resolve(storageKey).normalize();

        try {
            Files.createDirectories(storagePath.getParent());
            document.transferTo(storagePath);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Не удалось сохранить документ", e);
        }

        DocumentEntity entity = new DocumentEntity();
        entity.setOrderId(orderId);
        entity.setUploadedByUserId(uploadedByUserId);
        entity.setOriginalFileName(originalFileName);
        entity.setStorageKey(storageKey);
        entity.setMimeType(resolveMimeType(document));
        entity.setSizeBytes(document.getSize());
        entity.setIsDeleted(false);
        entity.setCreatedAt(LocalDateTime.now());
        DocumentEntity savedEntity = documentRepository.save(entity);
        outboxEventRepository.save(outboxEventFactory.documentStored(savedEntity));
        return savedEntity;
    }

    private UploadedDocumentResponse toResponse(DocumentEntity entity) {
        return new UploadedDocumentResponse(
                String.valueOf(entity.getId()),
                entity.getOriginalFileName(),
                entity.getMimeType(),
                entity.getSizeBytes(),
                entity.getCreatedAt(),
                null,
                Boolean.TRUE.equals(entity.getIsDeleted()),
                entity.getDeletedAt()
        );
    }

    private String resolveOriginalFileName(MultipartFile document) {
        String fileName = StringUtils.cleanPath(document.getOriginalFilename() == null ? "" : document.getOriginalFilename());
        if (fileName.isBlank()) {
            return "document.bin";
        }
        return fileName;
    }

    private String resolveMimeType(MultipartFile document) {
        String contentType = document.getContentType();
        if (contentType == null || contentType.isBlank()) {
            return "application/octet-stream";
        }
        return contentType;
    }
}
