package notification.persistence.notification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepo extends JpaRepository<NotificationEntity, UUID>{
    Optional<NotificationEntity> findByEventId(UUID eventId);
    List<NotificationEntity> findTop100ByStatusAndNextRetryAtBeforeOrderByNextRetryAtAsc(String status, LocalDateTime time);
    List<NotificationEntity> findTop100ByStatusOrderByCreatedAtAsc(String status);
    List<NotificationEntity> findTop100ByStatusAndUpdatedAtBeforeOrderByUpdatedAtAsc(String status, LocalDateTime time);
}
