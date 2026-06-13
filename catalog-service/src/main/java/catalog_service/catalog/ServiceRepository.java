package catalog_service.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceRepository extends JpaRepository<ServiceEntity, Long> {
    List<ServiceEntity> findByIsActiveTrueOrderBySortOrderAsc();
    Optional<ServiceEntity> findByCode(String code);
}
