package document_service.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class SaveResponse {
    private Long documentId;
    private UUID orderId;
    private String fileName;
    private String mimeType;
    private long size;
    private LocalDateTime uploadedAt;
    private String downloadUrl;
    private boolean isDeleted;
    private LocalDateTime deletedAt;
}
