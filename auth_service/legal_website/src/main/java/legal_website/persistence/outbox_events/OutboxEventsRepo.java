package legal_website.persistence.outbox_events;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventsRepo extends JpaRepository<OutboxEventEntity, UUID> {
    Optional<OutboxEventEntity> findByIdAndStatus(
            UUID id,
            String status
    );

    Optional<OutboxEventEntity> findByIdAndStatusAndProcessingToken(
            UUID id,
            String status,
            UUID processingToken
    );

    List<OutboxEventEntity> findTop100ByStatusOrderByCreatedAtAsc(String status);

    List<OutboxEventEntity> findTop100ByStatusAndNextRetryAtBeforeOrderByNextRetryAtAsc(String status, LocalDateTime time);

    List<OutboxEventEntity> findTop100ByStatusAndEventTypeOrderByCreatedAtAsc(
            String status,
            String eventType
    );

    List<OutboxEventEntity>
    findTop100ByStatusAndEventTypeAndNextRetryAtBeforeOrderByNextRetryAtAsc(
            String status,
            String eventType,
            LocalDateTime time
    );

    List<OutboxEventEntity> findTop100ByStatusAndEventTypeInOrderByCreatedAtAsc(
            String status,
            List<String> eventTypes
    );

    List<OutboxEventEntity>
    findTop100ByStatusAndEventTypeInAndNextRetryAtBeforeOrderByNextRetryAtAsc(
            String status,
            List<String> eventTypes,
            LocalDateTime time
    );

    /**
     * Одним запросом блокируем доступные события.
     *
     * SKIP LOCKED позволяет нескольким репликам auth-service распределить
     * события между собой без ожидания и без двойной отправки одной строки.
     */
    @Query(
            value = """
                    SELECT *
                    FROM outbox_events
                    WHERE event_type IN (:eventTypes)
                      AND (
                          status = 'NEW'
                          OR (
                              status = 'FAILED'
                              AND next_retry_at <= :now
                          )
                          OR (
                              status = 'PROCESSING'
                              AND processing_started_at <= :processingTimedOutBefore
                          )
                      )
                    ORDER BY created_at
                    LIMIT 100
                    FOR UPDATE SKIP LOCKED
                    """,
            nativeQuery = true
    )
    List<OutboxEventEntity> findDeletionEventsForUpdate(
            @Param("eventTypes") List<String> eventTypes,
            @Param("now") LocalDateTime now,
            @Param("processingTimedOutBefore")
            LocalDateTime processingTimedOutBefore
    );
}
