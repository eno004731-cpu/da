package lawyer_service.repo_entity.jwt;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JwtTokenRepo extends JpaRepository<JwtTokenEntity, UUID> {
    Optional<JwtTokenEntity> findByTokenHash(String tokenHash);

    List<JwtTokenEntity> findByLawyer_IdAndRevokedFalseAndSessionBlockedFalseAndExpiresAtAfter(
            UUID lawyerId,
            Instant now
    );
}
