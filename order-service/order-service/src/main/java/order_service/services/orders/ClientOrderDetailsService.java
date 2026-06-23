package order_service.services.orders;

import order_service.dto.response.ClientOrderDetailsResponse;
import order_service.persistence.order.OrderEntity;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ClientOrderDetailsService {
    private final ClientOrderAccessService clientOrderAccessService;
    private final OrderResponseMapper orderResponseMapper;

    public ClientOrderDetailsService(
            ClientOrderAccessService clientOrderAccessService,
            OrderResponseMapper orderResponseMapper
    ) {
        this.clientOrderAccessService = clientOrderAccessService;
        this.orderResponseMapper = orderResponseMapper;
    }

    public ClientOrderDetailsResponse getOrderDetails(UUID orderId, Long clientId) {
        OrderEntity order = clientOrderAccessService.getClientOrderOrThrow(orderId, clientId);
        return orderResponseMapper.toDetailsResponse(order);
    }
}
