package order_service.persistence.events.incoming;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IncomingEventRepo extends JpaRepository<IncomingEventEntity, UUID> {
    Optional<IncomingEventEntity> findByEventId(UUID eventId);

    boolean existsByEventId(UUID eventId);

    List<IncomingEventEntity> findTop100ByStatusOrderByReceivedAtAsc(
            IncomingEventEntity.Status status
    );

    List<IncomingEventEntity> findTop100ByStatusAndTopicOrderByReceivedAtAsc(
            IncomingEventEntity.Status status,
            String topic
    );

    List<IncomingEventEntity> findTop100ByStatusAndNextRetryAtBeforeOrderByNextRetryAtAsc(
            IncomingEventEntity.Status status,
            LocalDateTime time
    );
}
