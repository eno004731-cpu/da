package legal_website.EntityAndRepo.Auth;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepo extends JpaRepository<UserEntity,Long>{
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    Optional<UserEntity> findByEmail(String email);
}
