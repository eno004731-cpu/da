package legal_website.persistence.auth;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "oauth_accounts")
public class OAuthAccountEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Какой локальный пользователь владеет этой OAuth-связкой.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    // Провайдер нужен, чтобы одна таблица работала и для Google, и для будущих OAuth-провайдеров.
    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    // Уникальный id пользователя у внешнего провайдера, например Google "sub".
    @Column(name = "provider_user_id", nullable = false, length = 255, unique = true)
    private String providerUserId;

    // Email с OAuth-провайдера полезен для отладки и отображения, но не должен быть главным ключом связи.
    @Column(name = "provider_email", length = 255)
    private String providerEmail;

    @Column(name = "provider_email_verified", nullable = false)
    private boolean providerEmailVerified;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Это поле удобно обновлять при каждом успешном OAuth-логине.
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
}
