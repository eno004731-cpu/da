package order_service.dto.payload;

import java.util.UUID;

import lombok.Data;

@Data
public class DocumentToDeletePayload {
    private UUID eventId;
    private String documentId;
    private UUID orderId;
}
