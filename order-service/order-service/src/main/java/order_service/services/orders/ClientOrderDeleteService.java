package order_service.services.orders;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import order_service.persistence.order.OrderEntity;
import order_service.persistence.order.OrderRepo;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientOrderDeleteService {
    private final ClientOrderAccessService clientOrderAccessService;
    private final OrderRepo orderRepo;

    @Transactional
    public void deleteOrder(UUID orderId, Long clientId) {
        OrderEntity order = clientOrderAccessService.getClientOrderOrThrow(orderId, clientId);
        markDeletionInProgress(order);
        log.info("Order deletion started orderId={} clientId={}", orderId, clientId);

        try {
            LocalDateTime deletedAt = LocalDateTime.now();
            // Команда удаления документов заказа по orderId будет добавлена отдельным Kafka-flow.
            finalizeOrderDelete(order, deletedAt);
            log.info("Order deletion completed orderId={} clientId={}", orderId, clientId);
        } catch (Exception e) {
            markDeletionFailed(order, e.toString());
            // Stack trace нужен здесь, потому что это граница полного бизнес-сценария удаления.
            log.error("Order deletion failed orderId={} clientId={}", orderId, clientId, e);
            throw e;
        }
    }

    private void markDeletionInProgress(OrderEntity order) {
        order.setDeletionInProgress(true);
        order.setDeletionError(null);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepo.save(order);
    }

    private void finalizeOrderDelete(OrderEntity order, LocalDateTime deletedAt) {
        order.setIsDeleted(true);
        order.setDeletedAt(deletedAt);
        order.setDeletionInProgress(false);
        order.setDeletionError(null);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepo.save(order);
    }

    private void markDeletionFailed(OrderEntity order, String error) {
        order.setDeletionInProgress(false);
        order.setDeletionError(error);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepo.save(order);
    }
}
