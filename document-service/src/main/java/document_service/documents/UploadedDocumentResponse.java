package document_service.documents;

import java.time.LocalDateTime;

public record UploadedDocumentResponse(
        String id,
        String fileName,
        String mimeType,
        long size,
        LocalDateTime uploadedAt,
        String downloadUrl,
        boolean isDeleted,
        LocalDateTime deletedAt
) {
}
