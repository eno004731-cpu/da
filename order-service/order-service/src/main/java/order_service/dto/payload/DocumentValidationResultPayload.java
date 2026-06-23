package order_service.dto.payload;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Результат проверки связи документа с заказом.
 */
@Data
public class DocumentValidationResultPayload {
    private UUID eventId;
    private Long documentId;
    private UUID orderId;
    private Boolean validationPassed;
    private LocalDateTime validatedAt;
}
