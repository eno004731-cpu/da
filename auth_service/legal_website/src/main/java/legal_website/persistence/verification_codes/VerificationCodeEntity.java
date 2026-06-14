package legal_website.persistence.verification_codes;

import java.time.LocalDateTime;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import legal_website.persistence.auth.UserEntity;
import lombok.Data;

@Data
@Entity
@Table(name = "verification_codes")
public class VerificationCodeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity userId;
    @Column(name = "channel", nullable = false, length = 20)
    private String channel;
    @Column(name="purpose", nullable = false, length = 50)
    private String purpose;
    @Column(name="recipient", nullable = false, length = 255)
    private String recipient;
    @Column(name="code_hash", nullable = false, length = 255)
    private String codeHash;
    @Column(name="expires_at", nullable = false)
    private LocalDateTime expiresAt;
    @Column(name="consumed_at", nullable = true)
    private LocalDateTime consumedAt;
    @Column(name="attempt_count", nullable = false, precision = 10, scale = 0)
    private Integer attemptCount;
    @Column(name="max_attempts", nullable = false, precision = 10, scale = 0)
    private Integer maxAttempts;
    @Column(name="created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name="updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
