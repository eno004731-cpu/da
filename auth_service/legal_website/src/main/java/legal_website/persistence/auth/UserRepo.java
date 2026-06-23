package legal_website.persistence.auth;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepo extends JpaRepository<UserEntity,Long>{
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    Optional<UserEntity> findByEmail(String email);

    /**
     * Загружает пользователя и необязательный процесс удаления одним SQL-запросом.
     * LEFT JOIN нужен, потому что у активного пользователя процесса ещё может не быть.
     */
    @Query("""
            SELECT user
            FROM UserEntity user
            LEFT JOIN FETCH user.deletionProcess
            WHERE user.id = :userId
            """)
    Optional<UserEntity> findByIdWithDeletionProcess(@Param("userId") Long userId);
}
