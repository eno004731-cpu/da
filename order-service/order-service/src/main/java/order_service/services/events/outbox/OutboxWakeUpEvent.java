package order_service.services.events.outbox;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

// Локальное событие нужно только для сигнала "в outbox появилась новая работа".
@Data
@AllArgsConstructor
public class OutboxWakeUpEvent {
    private UUID outboxEventId;
}
