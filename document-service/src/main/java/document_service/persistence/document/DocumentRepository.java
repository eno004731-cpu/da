package document_service.persistence.document;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {
    List<DocumentEntity> findAllByOrderIdOrderByCreatedAtAsc(UUID orderId);

    Optional<DocumentEntity> findByIdAndOrderId(Long id, UUID orderId);

    Optional<DocumentEntity> findByStorageKey(String storageKey);
}
