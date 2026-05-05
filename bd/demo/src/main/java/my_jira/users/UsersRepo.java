package my_jira.users;


import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface UsersRepo  extends JpaRepository<UsersEntity, Long>{

    Optional<UsersEntity> findByEmail(String email);
    boolean existsByEmail(String email);
   
}
