package document_service.services.documents;

import document_service.persistence.document.DocumentEntity;
import document_service.persistence.document.DocumentRepository;
import document_service.persistence.document.DocumentValidationStatus;
import document_service.services.documents.store.DocumentFileStorage;
import lombok.RequiredArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Проверяет право клиента на скачивание и загружает файл из внутреннего storage.
 */
@Service
@RequiredArgsConstructor
public class DocumentDownloadService {
    private final DocumentRepository documentRepository;
    private final DocumentFileStorage documentFileStorage;

    @Transactional(readOnly = true)
    public DownloadedDocument download(UUID orderId, Long userId, Long documentId) {
        // Поиск сразу по заказу и пользователю не позволяет скачать чужой документ по ID.
        DocumentEntity entity = documentRepository
                .findByIdAndOrderIdAndUploadedByUserId(documentId, orderId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Документ не найден"
                ));

        if (Boolean.TRUE.equals(entity.getIsDeleted()) || entity.isDocumentDeleted()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Документ удалён");
        }

        if (entity.getValidationStatus() != DocumentValidationStatus.DOCUMENT_VALIDATED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Документ ещё не прошёл проверку"
            );
        }

        Resource resource = documentFileStorage.loadAsResource(entity.getStorageKey());
        return new DownloadedDocument(
                resource,
                entity.getOriginalFileName(),
                entity.getMimeType(),
                entity.getSizeBytes()
        );
    }

    /**
     * Данные, необходимые контроллеру для формирования HTTP-ответа с файлом.
     */
    @Data
    @AllArgsConstructor
    public static class DownloadedDocument {
        private Resource resource;
        private String fileName;
        private String mimeType;
        private long size;
    }
}
