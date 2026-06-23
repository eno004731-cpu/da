package document_service.services.documents;

import document_service.dto.response.UploadedDocumentResponse;
import document_service.persistence.document.DocumentEntity;
import document_service.persistence.document.DocumentValidationStatus;
import org.springframework.stereotype.Component;

/**
 * Преобразует persistence-модель документа в DTO внешнего контракта.
 */
@Component
public class DocumentResponseMapper {

    public UploadedDocumentResponse toResponse(DocumentEntity entity) {
        // Mapper не выполняет запросы и не содержит бизнес-логики: только переносит данные.
        UploadedDocumentResponse response = new UploadedDocumentResponse();
        response.setId(String.valueOf(entity.getId()));
        response.setFileName(entity.getOriginalFileName());
        response.setMimeType(entity.getMimeType());
        response.setSize(entity.getSizeBytes());
        response.setUploadedAt(entity.getCreatedAt());
        response.setDeleted(entity.getIsDeleted());
        response.setDeletedAt(entity.getDeletedAt());
        response.setValidationStatus(entity.getValidationStatus().name());

        // storageKey остаётся внутренней деталью document-service.
        // Клиент получает защищённый API endpoint только после успешной проверки.
        if (entity.getValidationStatus() == DocumentValidationStatus.DOCUMENT_VALIDATED
                && !Boolean.TRUE.equals(entity.getIsDeleted())
                && !entity.isDocumentDeleted()) {
                    //
            response.setDownloadUrl(
                    "/client/orders/%s/documents/%s/download".formatted(
                            entity.getOrderId(),
                            entity.getId()
                    )
            );
        }

        return response;
    }
}
