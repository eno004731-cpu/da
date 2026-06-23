package document_service.persistence.events.incoming;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, UUID> {
    List<ProcessedEventEntity> findAllByStatusAndEventType(String status, String eventType);

    Optional<ProcessedEventEntity> findByEventId(UUID eventId);

    boolean existsByEventId(UUID eventId);
}
