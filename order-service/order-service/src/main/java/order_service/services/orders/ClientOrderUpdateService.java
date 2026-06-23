package order_service.services.orders;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import order_service.dto.request.UpdateClientOrderRequest;
import order_service.dto.response.ClientOrderDetailsResponse;
import order_service.persistence.order.OrderEntity;
import order_service.persistence.order.OrderRepo;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientOrderUpdateService {
    private final ClientOrderAccessService clientOrderAccessService;
    private final ClientOrderDetailsService clientOrderDetailsService;
    private final OrderRepo orderRepo;

    @Transactional
    public ClientOrderDetailsResponse updateOrder(UUID orderId, Long clientId, UpdateClientOrderRequest request) {
        OrderEntity order = clientOrderAccessService.getClientOrderOrThrow(orderId, clientId);
        String description = request.getDescription().trim();

        order.setServiceCode(request.getServiceCode().trim());
        order.setClientName(request.getClientName().trim());
        order.setContact(request.getContact().trim());
        order.setCompanyName(normalizeNullable(request.getCompanyName()));
        order.setTitle(buildTitle(description));
        order.setProblemDescription(description);
        order.setUpdatedAt(LocalDateTime.now());
        order.setDeletionError(null);
        orderRepo.save(order);
        // Не логируем описание, имя и контакт клиента, так как это чувствительные данные.
        log.info("Order updated orderId={} clientId={} serviceCode={}",
                orderId, clientId, order.getServiceCode());

        return clientOrderDetailsService.getOrderDetails(orderId, clientId);
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String buildTitle(String description) {
        if (description.length() <= 255) {
            return description;
        }
        return description.substring(0, 252) + "...";
    }
}
