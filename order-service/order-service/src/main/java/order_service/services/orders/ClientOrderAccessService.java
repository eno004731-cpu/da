package order_service.services.orders;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import order_service.persistence.order.OrderEntity;
import order_service.persistence.order.OrderRepo;

@Service
@RequiredArgsConstructor
public class ClientOrderAccessService {
    private final OrderRepo orderRepo;

    public OrderEntity getClientOrderOrThrow(UUID orderId, Long clientId) {
        return orderRepo.findByIdAndClientId(orderId, clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Заказ не найден"));
    }
}
