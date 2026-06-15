package order_service.dto.payload;

import java.util.UUID;

import lombok.Data;

@Data
public class DocumentDeletedPayload {
    private UUID eventId;
    private String documentId;
    private UUID orderId;
    private String deletedAt;
    private Boolean isDeleted;
}
