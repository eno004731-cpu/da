package legal_website.EntityAndRepo.Auth;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OAuthAccountRepo extends JpaRepository<OAuthAccountEntity, Long> {
    boolean existsByProviderAndProviderUserId(String provider, String providerUserId);

    Optional<OAuthAccountEntity> findByProviderAndProviderUserId(String provider, String providerUserId);

    Optional<OAuthAccountEntity> findByUserAndProvider(UserEntity user, String provider);

    List<OAuthAccountEntity> findAllByUser(UserEntity user);
    boolean deleteAllByUser(UserEntity user);
}
