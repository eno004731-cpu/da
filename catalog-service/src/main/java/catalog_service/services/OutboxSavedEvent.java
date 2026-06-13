package catalog_service.services;

import java.util.UUID;

// Маркер события: в catalog outbox успешно сохранили ответное сообщение.
public record OutboxSavedEvent(UUID outboxEventId) {
}
