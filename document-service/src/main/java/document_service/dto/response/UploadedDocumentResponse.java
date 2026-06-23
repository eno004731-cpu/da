package document_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UploadedDocumentResponse {
    private String id;
    private String fileName;
    private String mimeType;
    private long size;
    private LocalDateTime uploadedAt;
    private String downloadUrl;
    private boolean isDeleted;
    private LocalDateTime deletedAt;
    private String validationStatus;
}
