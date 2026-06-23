package legal_website.persistence.deletion;

import legal_website.persistence.auth.UserDeletionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserDeletionProcessRepo
        extends JpaRepository<UserDeletionProcessEntity, UUID> {

    Optional<UserDeletionProcessEntity> findByUserId(Long userId);

    /**
     * Scheduler сможет выбирать процессы, для которых наступило время повтора.
     */
    List<UserDeletionProcessEntity>
    findTop100ByStatusAndNextRetryAtBeforeOrderByNextRetryAtAsc(
            UserDeletionStatus status,
            LocalDateTime now
    );
}
