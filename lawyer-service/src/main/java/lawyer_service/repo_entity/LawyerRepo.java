package lawyer_service.repo_entity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import io.lettuce.core.dynamic.annotation.Param;

public interface LawyerRepo extends JpaRepository<LawyerEntity, UUID> {
        @Query(value = """
                SELECT *
                FROM lawyers
                WHERE email = :email AND status = ACTIVE
                ORDER BY created_at
                """, nativeQuery = true)
        Optional<LawyerEntity> findByEmailAndIsActiveTrue(@Param("email") String email);
        boolean existsByPhone(String phone);
        boolean existsByEmail(String email);
        @Query(value = """
                SELECT *
                FROM lawyers
                WHERE email = :email AND status = 'ACTIVE' OR status = 'PENDING_VERIFICATION'
                ORDER BY created_at 
                FOR UPDATE LOCK SKIP
                """, nativeQuery = true)
        Optional<LawyerEntity> findByEmailAndLock(@Param("email") String email);
        @Query(
                value = """
                               SELECT *
                               FROM lawyers
                               WHERE status = 'PENDING_VERIFICATION'
                               ORDER BY created_at 
                               FOR UPDATE LOCK SKIP
                                """,
                nativeQuery = true
        )
        List<LawyerEntity> findAllLawyerByStatus();
}
