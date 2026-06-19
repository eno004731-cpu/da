package document_service.dto.payload;

import java.util.UUID;

import lombok.Data;

@Data
public class DocumentCheckOrder {
    private UUID orderId;
    private UUID eventId;
    private Long documentId;
}
