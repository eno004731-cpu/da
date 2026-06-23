package legal_website.services.outbox;

import legal_website.persistence.outbox_events.OutboxEventEntity;
import legal_website.persistence.outbox_events.OutboxEventsRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Единое место для изменения технических статусов outbox-событий.
 *
 * Publisher решает, что отправлять, а этот сервис решает,
 * как сохранять PROCESSING, PUBLISHED, FAILED и DEAD.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxEventStatusService {
    private static final int MAX_RETRY_COUNT = 5;
    private static final int RETRY_DELAY_SECONDS = 5;

    private final OutboxEventsRepo outboxEventsRepo;

    @Transactional
    public void markProcessing(OutboxEventEntity event) {
        applyProcessingState(event, LocalDateTime.now());
    }

    /**
     * Позволяет claim-service назначить одинаковое время lease всей пачке.
     */
    @Transactional
    public void markProcessing(
            OutboxEventEntity event,
            LocalDateTime processingStartedAt
    ) {
        applyProcessingState(event, processingStartedAt);
    }

    /**
     * Обновляем только PROCESSING-событие, чтобы старый callback
     * не перезаписал более новое состояние.
     */
    @Transactional
    public void markPublished(UUID eventId) {
        outboxEventsRepo.findByIdAndStatus(eventId, "PROCESSING")
                .ifPresent(this::applyPublishedState);
    }

    /**
     * Delete publisher передаёт токен конкретной попытки.
     * Запоздалый callback старой попытки не найдёт строку и будет проигнорирован.
     */
    @Transactional
    public void markPublished(UUID eventId, UUID processingToken) {
        outboxEventsRepo
                .findByIdAndStatusAndProcessingToken(
                        eventId,
                        "PROCESSING",
                        processingToken
                )
                .ifPresent(this::applyPublishedState);
    }

    /**
     * Вариант для асинхронного Kafka callback.
     */
    @Transactional
    public void markFailed(UUID eventId, String errorMessage) {
        outboxEventsRepo.findByIdAndStatus(eventId, "PROCESSING")
                .ifPresent(event -> {
                    applyFailedState(event, errorMessage);
                    outboxEventsRepo.save(event);
                    logFailure(event);
                });
    }

    @Transactional
    public void markFailed(
            UUID eventId,
            UUID processingToken,
            String errorMessage
    ) {
        outboxEventsRepo
                .findByIdAndStatusAndProcessingToken(
                        eventId,
                        "PROCESSING",
                        processingToken
                )
                .ifPresent(event -> {
                    applyFailedState(event, errorMessage);
                    outboxEventsRepo.save(event);
                    logFailure(event);
                });
    }

    /**
     * Вариант для ошибки до отправки, например при чтении payload.
     */
    @Transactional
    public void markFailed(OutboxEventEntity event, String errorMessage) {
        applyFailedState(event, errorMessage);
        outboxEventsRepo.save(event);
        logFailure(event);
    }

    /**
     * DEAD применяется для постоянной ошибки, которую retry не исправит.
     */
    @Transactional
    public void markDead(OutboxEventEntity event, String errorMessage) {
        event.setStatus("DEAD");
        event.setLastError(errorMessage);
        event.setLastErrorAt(LocalDateTime.now());
        event.setRetryCount(MAX_RETRY_COUNT);
        event.setNextRetryAt(null);
        event.setProcessingStartedAt(null);
        event.setProcessingToken(null);
        outboxEventsRepo.save(event);

        log.error(
                "Outbox event marked dead eventId={} eventType={} error={}",
                event.getId(),
                event.getEventType(),
                errorMessage
        );
    }

    private void applyFailedState(
            OutboxEventEntity event,
            String errorMessage
    ) {
        int currentRetryCount = event.getRetryCount() == null
                ? 0
                : event.getRetryCount();
        int nextRetryCount = currentRetryCount + 1;

        event.setLastError(errorMessage);
        event.setLastErrorAt(LocalDateTime.now());
        event.setRetryCount(nextRetryCount);
        event.setPublishedAt(null);
        event.setProcessingStartedAt(null);
        event.setProcessingToken(null);

        if (nextRetryCount >= MAX_RETRY_COUNT) {
            event.setStatus("DEAD");
            event.setNextRetryAt(null);
        } else {
            event.setStatus("FAILED");
            event.setNextRetryAt(
                    LocalDateTime.now().plusSeconds(RETRY_DELAY_SECONDS)
            );
        }
    }

    private void applyProcessingState(
            OutboxEventEntity event,
            LocalDateTime processingStartedAt
    ) {
        event.setStatus("PROCESSING");
        event.setNextRetryAt(null);
        event.setProcessingStartedAt(processingStartedAt);
        event.setProcessingToken(UUID.randomUUID());
        outboxEventsRepo.save(event);
    }

    private void applyPublishedState(OutboxEventEntity event) {
        event.setStatus("PUBLISHED");
        event.setNextRetryAt(null);
        event.setProcessingStartedAt(null);
        event.setProcessingToken(null);
        event.setPublishedAt(LocalDateTime.now());
        outboxEventsRepo.save(event);

        log.info(
                "Outbox event published eventId={} eventType={}",
                event.getId(),
                event.getEventType()
        );
    }

    private void logFailure(OutboxEventEntity event) {
        log.warn(
                "Outbox event failed eventId={} eventType={} status={} retryCount={} error={}",
                event.getId(),
                event.getEventType(),
                event.getStatus(),
                event.getRetryCount(),
                event.getLastError()
        );
    }
}
