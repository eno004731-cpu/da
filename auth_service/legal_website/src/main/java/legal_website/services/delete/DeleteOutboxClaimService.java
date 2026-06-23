package legal_website.services.delete;

import legal_website.persistence.outbox_events.OutboxEventEntity;
import legal_website.persistence.outbox_events.OutboxEventsRepo;
import legal_website.services.outbox.OutboxEventStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Выбирает и блокирует delete-outbox события для publisher.
 *
 * Здесь находится lease-логика, а изменение статусов делегируется
 * в OutboxEventStatusService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeleteOutboxClaimService {
    private static final String PROCESSING = "PROCESSING";
    private static final String DEAD = "DEAD";
    private static final String PROCESSING_TIMEOUT_ERROR =
            "Processing timeout: callback was not completed";

    private static final List<String> DELETE_EVENT_TYPES =
            Arrays.stream(DeleteOutboxEventType.values())
                    .map(Enum::name)
                    .toList();

    private final OutboxEventsRepo outboxEventsRepo;
    private final OutboxEventStatusService outboxEventStatusService;

    /**
     * Одним запросом получает NEW, готовые FAILED и зависшие PROCESSING.
     * FOR UPDATE SKIP LOCKED распределяет строки между репликами сервиса.
     */
    @Transactional
    public List<OutboxEventEntity> claimAvailableEvents(
            LocalDateTime now,
            LocalDateTime processingTimedOutBefore
    ) {
        List<OutboxEventEntity> lockedEvents =
                outboxEventsRepo.findDeletionEventsForUpdate(
                        DELETE_EVENT_TYPES,
                        now,
                        processingTimedOutBefore
                );
        List<OutboxEventEntity> claimedEvents = new ArrayList<>();

        for (OutboxEventEntity event : lockedEvents) {
            if (PROCESSING.equals(event.getStatus())) {
                recoverTimedOutEvent(event);

                if (DEAD.equals(event.getStatus())) {
                    continue;
                }
            }

            outboxEventStatusService.markProcessing(event, now);
            claimedEvents.add(event);
        }

        return claimedEvents;
    }

    private void recoverTimedOutEvent(OutboxEventEntity event) {
        outboxEventStatusService.markFailed(
                event,
                PROCESSING_TIMEOUT_ERROR
        );

        log.warn(
                "Timed out delete event recovered eventId={} eventType={} retryCount={} status={}",
                event.getId(),
                event.getEventType(),
                event.getRetryCount(),
                event.getStatus()
        );
    }
}
