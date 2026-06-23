package legal_website.persistence.auth;



import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import legal_website.persistence.deletion.UserDeletionProcessEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Table(name = "users")
@Entity
@Data
public class UserEntity {
    @Id()
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "role", length = 50,nullable = false)
    private String role;
    @Column(name = "full_name",length = 255,nullable = false)
    private String fullName;
    @Column(name = "email",unique = true)
    private String email;
    @Column(name = "phone",length = 20,unique = true)
    private String phone;
    @Column(name = "company_name",length = 255)
    private String companyName;
    @Column(name = "password_hash",length = 255,nullable = false)
    private String passwordHash;
    @Column(name = "email_verified",nullable = false)
    private boolean emailVerified;
    @Column(name = "is_active",nullable = false)
    private boolean isActive;
    @Enumerated(EnumType.STRING)
    @Column(name = "deletion_status", length = 40, nullable = false)
    private UserDeletionStatus deletionStatus = UserDeletionStatus.ACTIVE;

    /**
     * Детали процесса находятся в отдельной таблице, но доступны через пользователя.
     * LAZY не загружает процесс там, где приложению нужен только сам пользователь.
     */
    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private UserDeletionProcessEntity deletionProcess;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}
