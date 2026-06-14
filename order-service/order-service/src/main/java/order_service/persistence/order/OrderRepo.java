package order_service.persistence.order;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepo extends JpaRepository<OrderEntity,UUID>{
    Optional<OrderEntity> findByIdAndClientId(UUID id,Long userId);
}
