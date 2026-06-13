package catalog_service.entityAndRepo.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxEventRepo extends JpaRepository<OutboxEventEntity, UUID> {
    // Ищем событие в ожидаемом статусе, чтобы безопасно делать переход состояния.
    Optional<OutboxEventEntity> findByIdAndStatus(UUID id, String status);

    // Берём новую пачку событий на публикацию в порядке создания.
    List<OutboxEventEntity> findTop100ByStatusOrderByCreatedAtAsc(String status);

    // Берём события, которые уже можно повторно отправлять после ошибки.
    List<OutboxEventEntity> findTop100ByStatusAndNextRetryAtBeforeOrderByNextRetryAtAsc(
            String status,
            LocalDateTime time
    );
}
