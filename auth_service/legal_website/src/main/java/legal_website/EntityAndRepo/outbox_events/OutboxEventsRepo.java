package legal_website.EntityAndRepo.outbox_events;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventsRepo extends JpaRepository<OutboxEventEntity, UUID> {
    Optional<OutboxEventEntity> findByIdAndStatus(UUID id, String status);
    List<OutboxEventEntity> findTop100ByStatusOrderByCreatedAtAsc(String status);
    List<OutboxEventEntity> findTop100ByStatusAndNextRetryAtBeforeOrderByNextRetryAtAsc(String status, LocalDateTime time);
}
