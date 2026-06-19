package document_service.services.documents;

import document_service.dto.response.UploadedDocumentResponse;
import document_service.persistence.document.DocumentEntity;
import org.springframework.stereotype.Component;

/**
 * Преобразует persistence-модель документа в DTO внешнего контракта.
 */
@Component
public class DocumentResponseMapper {

    public UploadedDocumentResponse toResponse(DocumentEntity entity) {
        // Mapper не выполняет запросы и не содержит бизнес-логики: только переносит данные.
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
}
