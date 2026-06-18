package order_service.services;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import order_service.persistence.order.OrderEntity;
import order_service.persistence.order.OrderRepo;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckOrder {
    private final OrderRepo orderRepo;
    public boolean checkOrder(UUID orderId,Long userId){
        // Без идентификаторов нельзя проверить принадлежность конкретного заказа пользователю.
        if (orderId == null || userId == null) {
            log.warn("Order ID and user ID must be provided");
            return false;
        }
        OrderEntity order = orderRepo.findByIdAndClientId(orderId, userId)
            // ResponseStatusException сообщает Spring, какой HTTP-статус вернуть фронтенду.
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Заказ не найден"));

        // Boolean.TRUE.equals безопасен, даже если старые данные содержат null.
        if (Boolean.TRUE.equals(order.getIsDeleted())) {
            log.warn("The order has been deleted, meaning the documents will not be saved ");
            return false;
        }
        if (Boolean.TRUE.equals(order.getDeletionInProgress())) {
            log.warn("The order is in the process of being deleted, which means the documents will not be saved");
            return false;
        }

        return true;
    }
}
