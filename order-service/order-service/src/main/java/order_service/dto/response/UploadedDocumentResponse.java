package order_service.dto.response;

import java.time.LocalDateTime;

import lombok.Data;

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
}
