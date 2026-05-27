package legal_website.EntityAndRepo.Jwt;

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
import legal_website.EntityAndRepo.Auth.UserEntity;
import lombok.Data;

@Data
@Entity
@Table(name = "refresh_tokens")
public class JwtEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",nullable = false)
    private UserEntity user;
    @Column(name = "token_hash",nullable = false,length = 255)
    private String tokenHash;
    @Column(name = "expires_at",nullable = false)
    private LocalDateTime expiresAt;
    @Column(name = "created_at",nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

}
