package notification.persistence.events;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepo extends JpaRepository<EventEntity, UUID> {
    Optional<EventEntity> findByEventId(UUID eventId);
    boolean existsByEventId(UUID eventId);
}
