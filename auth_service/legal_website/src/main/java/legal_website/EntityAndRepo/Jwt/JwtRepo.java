package legal_website.EntityAndRepo.Jwt;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import legal_website.EntityAndRepo.Auth.UserEntity;

public interface JwtRepo extends JpaRepository <JwtEntity, Long>{
    Optional<JwtEntity> findByTokenHash(String token);
    List<JwtEntity> findAllByUser(UserEntity user);
    boolean deleteAllByUser(UserEntity user);
}
