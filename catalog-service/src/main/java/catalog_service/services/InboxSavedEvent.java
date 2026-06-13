package catalog_service.services;

import java.util.UUID;

// Маркер события: в catalog inbox успешно сохранили входящее сообщение.
public record InboxSavedEvent(UUID inboxEventId) {
}
