package document_service.persistence.document;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {
    List<DocumentEntity> findAllByOrderIdOrderByCreatedAtAsc(UUID orderId);

    List<DocumentEntity> findAllByOrderIdAndUploadedByUserIdOrderByCreatedAtAsc(
            UUID orderId,
            Long uploadedByUserId
    );

    List<DocumentEntity> findAllByIsDocumentDeletedTrueAndIsDeletedFalse();

    List<DocumentEntity>
    findTop100ByValidationStatusAndIsDeletedFalseAndIsDocumentDeletedFalseAndValidationRequestedAtLessThanEqualOrderByValidationRequestedAtAsc(
            DocumentValidationStatus validationStatus,
            LocalDateTime requestedBefore
    );

    Optional<DocumentEntity> findByIdAndOrderId(Long id, UUID orderId);

    Optional<DocumentEntity> findByStorageKey(String storageKey);
    Optional<DocumentEntity> findByIdAndOrderIdAndUploadedByUserId(Long id, UUID orderId,Long UserId);
}
