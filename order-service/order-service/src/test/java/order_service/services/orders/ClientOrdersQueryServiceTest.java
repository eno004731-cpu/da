package order_service.services.orders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import order_service.dto.response.ClientOrderSummaryResponse;
import order_service.persistence.order.OrderEntity;
import order_service.persistence.order.OrderRepo;

@ExtendWith(MockitoExtension.class)
class ClientOrdersQueryServiceTest {
    @Mock
    private OrderRepo orderRepo;

    @Spy
    private OrderResponseMapper orderResponseMapper = new OrderResponseMapper();

    @InjectMocks
    private ClientOrdersQueryService service;

    @Test
    void getClientOrders_returnsMappedClientOrders() {
        OrderEntity order = order("CONSULT", "Consultation");
        when(orderRepo.findAllByClientIdOrderByCreateAtDesc(7L)).thenReturn(List.of(order));

        List<ClientOrderSummaryResponse> response = service.getClientOrders(7L);

        assertEquals(1, response.size());
        assertEquals(order.getId(), response.get(0).getId());
        assertEquals("CONSULT", response.get(0).getServiceCode());
        assertEquals("Consultation", response.get(0).getTitle());
        assertEquals(0, response.get(0).getRevisionCount());
    }

    private OrderEntity order(String serviceCode, String title) {
        LocalDateTime now = LocalDateTime.now();
        OrderEntity order = new OrderEntity();
        order.setId(UUID.randomUUID());
        order.setServiceCode(serviceCode);
        order.setServiceName("Service " + serviceCode);
        order.setTitle(title);
        order.setStatus("ON_REVIEW");
        order.setCreateAt(now);
        order.setUpdatedAt(now);
        return order;
    }
}
