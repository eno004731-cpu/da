package order_service.persistence.document;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderDocumentMetadataRepo extends JpaRepository<OrderDocumentMetadataEntity, Long> {
    List<OrderDocumentMetadataEntity> findAllByOrderIdOrderByUploadedAtAsc(UUID orderId);

    Optional<OrderDocumentMetadataEntity> findByDocumentId(String documentId);

    Optional<OrderDocumentMetadataEntity> findByOrderIdAndDocumentId(UUID orderId, String documentId);

    boolean existsByDocumentId(String documentId);
}
