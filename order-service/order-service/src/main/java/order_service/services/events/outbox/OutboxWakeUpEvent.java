package order_service.services.events.outbox;

import java.util.UUID;

// Локальное событие нужно только для сигнала "в outbox появилась новая работа".
public record OutboxWakeUpEvent(UUID outboxEventId) {
}
