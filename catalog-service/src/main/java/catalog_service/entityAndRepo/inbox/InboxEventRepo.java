package catalog_service.entityAndRepo.inbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InboxEventRepo extends JpaRepository<InboxEventEntity, UUID> {
    // Проверяем, не обрабатывали ли мы уже событие с тем же внешним event_id.
    Optional<InboxEventEntity> findByEventId(UUID eventId);
    boolean existsByEventId(UUID eventId);
    // Ищем запись в ожидаемом статусе, чтобы делать безопасный переход состояния.
    Optional<InboxEventEntity> findByIdAndStatus(UUID id, String status);
    List<InboxEventEntity> findTop100ByStatusOrderByCreatedAtAsc(String status);
    // Берём пачку событий, которые уже можно повторно отправить в обработку.
    List<InboxEventEntity> findTop100ByStatusAndNextRetryAtBeforeOrderByNextRetryAtAsc(
            String status,
            LocalDateTime time
    );
}
