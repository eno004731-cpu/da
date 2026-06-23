package document_service.dto.payload;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Контракт результата проверки, опубликованный order-service.
 */
@Data
public class DocumentValidationResultPayload {
    @NotNull
    private UUID eventId;

    @NotNull
    private Long documentId;

    @NotNull
    private UUID orderId;

    @NotNull
    private Boolean validationPassed;

    @NotNull
    private LocalDateTime validatedAt;
}
