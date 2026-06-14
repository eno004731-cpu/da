package order_service.services.orders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import order_service.dto.request.CreateOrderRequest;
import order_service.dto.response.CreateOrderResponse;
import order_service.persistence.order.OrderEntity;
import order_service.persistence.order.OrderRepo;
import order_service.services.catalog.ServiceNameOutboxService;

@ExtendWith(MockitoExtension.class)
class CreateOrderServiceTest {

    @Mock
    private OrderRepo orderRepo;

    @Mock
    private ServiceNameOutboxService serviceNameOutboxService;

    @InjectMocks
    private CreateOrderService service;

    @Test
    void createOrder_savesOrderAndOutboxEventAndReturnsResponse() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setClientName("Nikita");
        request.setCompanyName("Acme");
        request.setContact("+79990000000");
        request.setServiceCode("CONSULT");
        request.setDescription("Need legal advice");
        UUID orderId = UUID.randomUUID();

        when(orderRepo.save(any(OrderEntity.class))).thenAnswer(invocation -> {
            OrderEntity order = invocation.getArgument(0);
            // В unit-тесте сами эмулируем поведение persistence слоя, который вернёт id.
            order.setId(orderId);
            return order;
        });

        CreateOrderResponse response = service.createOrder(request, 42L);

        ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepo).save(orderCaptor.capture());
        OrderEntity savedOrder = orderCaptor.getValue();
        assertEquals(42L, savedOrder.getClientId());
        assertEquals("CONSULT", savedOrder.getServiceCode());
        assertEquals("ON_REVIEW", savedOrder.getStatus());
        assertEquals("Need legal advice", savedOrder.getTitle());
        assertEquals("Need legal advice", savedOrder.getProblemDescription());

        assertEquals(orderId, response.getId());
        assertEquals(orderId, response.getOrderId());
        assertEquals("ON_REVIEW", response.getStatus());
        verify(serviceNameOutboxService).createServiceNameRequestedEvent(savedOrder);
        verify(orderRepo, times(1)).save(any(OrderEntity.class));
    }
}
