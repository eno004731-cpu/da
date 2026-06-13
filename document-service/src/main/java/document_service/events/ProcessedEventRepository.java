package document_service.events;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, UUID> {
    Optional<ProcessedEventEntity> findByEventId(UUID eventId);

    boolean existsByEventId(UUID eventId);
}
