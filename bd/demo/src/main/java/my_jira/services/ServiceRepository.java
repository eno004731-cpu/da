

package my_jira.services;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRepository extends JpaRepository<ServiceEntity, Long> {

    List<ServiceEntity> findByIsActiveTrueOrderBySortOrderAsc();
    ServiceEntity findByCode(String code);
}
