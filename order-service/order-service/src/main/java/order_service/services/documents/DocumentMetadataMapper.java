package order_service.services.documents;

import order_service.dto.response.UploadedDocumentResponse;
import order_service.persistence.document.OrderDocumentMetadataEntity;
import org.springframework.stereotype.Component;

@Component
public class DocumentMetadataMapper {
    public UploadedDocumentResponse toResponse(OrderDocumentMetadataEntity entity) {
        UploadedDocumentResponse response = new UploadedDocumentResponse();
        response.setId(entity.getDocumentId());
        response.setFileName(entity.getFileName());
        response.setMimeType(entity.getMimeType());
        response.setSize(entity.getSizeBytes());
        response.setUploadedAt(entity.getUploadedAt());
        response.setDownloadUrl(null);
        response.setDeleted(Boolean.TRUE.equals(entity.getIsDeleted()));
        response.setDeletedAt(entity.getDeletedAt());
        return response;
    }
}
