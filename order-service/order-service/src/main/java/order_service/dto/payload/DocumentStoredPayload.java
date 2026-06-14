package order_service.dto.payload;

import lombok.Data;

import java.util.UUID;

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
}
