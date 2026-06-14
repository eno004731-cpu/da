package legal_website.persistence.jwt;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import legal_website.persistence.auth.UserEntity;

public interface JwtRepo extends JpaRepository <JwtEntity, Long>{
    Optional<JwtEntity> findByTokenHash(String token);
    List<JwtEntity> findAllByUser(UserEntity user);
    boolean deleteAllByUser(UserEntity user);
}
