package legal_website.persistence.auth;



import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

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
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}
