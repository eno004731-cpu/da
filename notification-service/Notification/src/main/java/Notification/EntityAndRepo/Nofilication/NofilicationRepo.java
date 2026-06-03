package Notification.EntityAndRepo.Nofilication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NofilicationRepo extends JpaRepository<NofilicationEntity, UUID>{
    Optional<NofilicationEntity> findByEventId(UUID eventId);
    List<NofilicationEntity> findTop100ByStatusAndNextRetryAtBeforeOrderByNextRetryAtAsc(String status, LocalDateTime time);
    List<NofilicationEntity> findTop100ByStatusOrderByCreatedAtAsc(String status);
}
