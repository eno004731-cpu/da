package document_service.persistence.events.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {
    Optional<OutboxEventEntity> findByIdAndStatus(UUID id, String status);

    List<OutboxEventEntity> findTop100ByStatusOrderByCreatedAtAsc(String status);

    List<OutboxEventEntity> findTop100ByStatusAndNextRetryAtBeforeOrderByNextRetryAtAsc(
            String status,
            LocalDateTime time
    );

    boolean existsByAggregateIdAndEventTypeAndStatusIn(
            String aggregateId,
            String eventType,
            Collection<String> statuses
    );
}
