package order_service.services.orders;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import order_service.dto.response.ClientOrderSummaryResponse;
import order_service.persistence.order.OrderRepo;

@Service
@RequiredArgsConstructor
public class ClientOrdersQueryService {
    private final OrderRepo orderRepo;
    private final OrderResponseMapper orderResponseMapper;

    public List<ClientOrderSummaryResponse> getClientOrders(Long clientId) {
        return orderRepo.findAllByClientIdOrderByCreateAtDesc(clientId).stream()
                .map(orderResponseMapper::toSummaryResponse)
                .toList();
    }
}
