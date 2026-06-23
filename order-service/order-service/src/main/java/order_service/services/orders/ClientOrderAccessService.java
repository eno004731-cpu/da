package order_service.services.orders;

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
public class ClientOrderAccessService {
    private final OrderRepo orderRepo;

    public OrderEntity getClientOrderOrThrow(UUID orderId, Long clientId) {
        return orderRepo.findByIdAndClientId(orderId, clientId)
                .orElseThrow(() -> {
                    // Одинаковый 404 не раскрывает, существует ли чужой заказ.
                    log.warn("Client order access denied or order not found orderId={} clientId={}", orderId, clientId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Заказ не найден");
                });
    }
}
