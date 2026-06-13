package order_service.EntityAndRepo.events.incoming;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IncomingEventRepo extends JpaRepository<IncomingEventEntity, UUID> {
    Optional<IncomingEventEntity> findByEventId(UUID eventId);

    boolean existsByEventId(UUID eventId);

    List<IncomingEventEntity> findTop100ByStatusOrderByReceivedAtAsc(String status);

    List<IncomingEventEntity> findTop100ByStatusAndNextRetryAtBeforeOrderByNextRetryAtAsc(
            String status,
            LocalDateTime time
    );
}
