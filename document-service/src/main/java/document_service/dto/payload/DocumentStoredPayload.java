package document_service.dto.payload;

import lombok.Data;

import java.util.UUID;

import document_service.persistence.document.DocumentEntity;

@Data
public class DocumentStoredPayload {
    private UUID eventId;
    private String documentId;
    private UUID orderId;
    private Long uploadedByUserId;
    private String fileName;
    private String mimeType;
    private Long sizeBytes;
    private String uploadedAt;
    private Boolean isDeleted;
    private DocumentEntity.Status status;
    private DocumentEntity.ValidationStatus validationStatus;
}
