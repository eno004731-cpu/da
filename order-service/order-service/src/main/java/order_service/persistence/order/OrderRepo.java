package order_service.persistence.order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepo extends JpaRepository<OrderEntity,UUID>{
    Optional<OrderEntity> findByIdAndClientIdAndIsDeletedFalseAndDeletionInProgressFalse(UUID id,Long userId);
    List<OrderEntity> findAllByClientIdAndIsDeletedFalseAndDeletionInProgressFalseOrderByCreateAtDesc(Long clientId);

    default Optional<OrderEntity> findByIdAndClientId(UUID id, Long userId) {
        return findByIdAndClientIdAndIsDeletedFalseAndDeletionInProgressFalse(id, userId);
    }

    default List<OrderEntity> findAllByClientIdOrderByCreateAtDesc(Long clientId) {
        return findAllByClientIdAndIsDeletedFalseAndDeletionInProgressFalseOrderByCreateAtDesc(clientId);
    }
    // Имя части IsDeleted должно точно совпадать с полем isDeleted в OrderEntity.
    boolean existsByIdAndClientIdAndIsDeletedFalseAndDeletionInProgressFalse(UUID id, Long userId);
}
