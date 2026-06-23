package document_service.dto.payload;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DocumentToDeletePayload {
    // Идентификатор события обязателен для идемпотентной обработки Kafka-сообщения.
    @NotNull(message = "eventId не должен быть null")
    private UUID eventId;

    // Пустая строка не является корректным идентификатором документа.
    @NotBlank(message = "documentId не должен быть пустым")
    private String documentId;

    // Идентификатор заказа нужен, чтобы определить владельца удаляемого документа.
    @NotNull(message = "orderId не должен быть null")
    private UUID orderId;
}
