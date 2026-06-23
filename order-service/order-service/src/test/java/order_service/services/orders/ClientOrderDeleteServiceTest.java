package order_service.services.orders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import order_service.persistence.order.OrderEntity;
import order_service.persistence.order.OrderRepo;

@ExtendWith(MockitoExtension.class)
class ClientOrderDeleteServiceTest {
    @Mock
    private ClientOrderAccessService clientOrderAccessService;

    @Mock
    private OrderRepo orderRepo;

    @InjectMocks
    private ClientOrderDeleteService service;

    @Test
    void deleteOrder_finalizesSoftDelete() {
        UUID orderId = UUID.randomUUID();
        OrderEntity order = new OrderEntity();
        order.setIsDeleted(false);
        when(clientOrderAccessService.getClientOrderOrThrow(orderId, 7L)).thenReturn(order);

        service.deleteOrder(orderId, 7L);

        assertEquals(true, order.getIsDeleted());
        assertEquals(false, order.getDeletionInProgress());
        assertNotNull(order.getDeletedAt());
    }

}
