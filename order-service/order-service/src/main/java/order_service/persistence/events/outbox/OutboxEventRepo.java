package order_service.persistence.events.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxEventRepo extends JpaRepository<OutboxEventEntity, UUID> {
    Optional<OutboxEventEntity> findByIdAndStatus(UUID id, String status);

    List<OutboxEventEntity> findTop100ByStatusOrderByCreatedAtAsc(String status);

    List<OutboxEventEntity> findTop100ByStatusAndEventTypeOrderByCreatedAtAsc(
            String status,
            String eventType
    );

    List<OutboxEventEntity> findTop100ByStatusAndNextRetryAtBeforeOrderByNextRetryAtAsc(
            String status,
            LocalDateTime time
    );

    List<OutboxEventEntity> findTop100ByStatusAndEventTypeAndNextRetryAtBeforeOrderByNextRetryAtAsc(
            String status,
            String eventType,
            LocalDateTime time
    );
}
