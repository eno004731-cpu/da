package document_service.events;

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
