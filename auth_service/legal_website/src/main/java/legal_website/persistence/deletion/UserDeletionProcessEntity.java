package legal_website.persistence.deletion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import legal_website.persistence.auth.UserDeletionStatus;
import legal_website.persistence.auth.UserEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Хранит прогресс распределённого удаления данных пользователя.
 * Одна запись соответствует одному пользователю.
 */
@Entity
@Table(name = "user_deletion_process")
@Data
public class UserDeletionProcessEntity {
    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    // Исключаем обратную ссылку из Lombok, иначе toString/equals зациклится:
    // user -> deletionProcess -> user.
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 40, nullable = false)
    private UserDeletionStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
