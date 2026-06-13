package order_service.Services.events;

import java.util.UUID;

// Локальное событие нужно только для сигнала "в outbox появилась новая работа".
public record OutboxWakeUpEvent(UUID outboxEventId) {
}
