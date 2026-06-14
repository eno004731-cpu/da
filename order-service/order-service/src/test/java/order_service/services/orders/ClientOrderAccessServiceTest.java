package order_service.services.orders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import order_service.persistence.order.OrderEntity;
import order_service.persistence.order.OrderRepo;

@ExtendWith(MockitoExtension.class)
class ClientOrderAccessServiceTest {
    @Mock
    private OrderRepo orderRepo;

    @InjectMocks
    private ClientOrderAccessService service;

    @Test
    void getClientOrderOrThrow_returnsOwnedOrder() {
        UUID orderId = UUID.randomUUID();
        OrderEntity order = new OrderEntity();
        when(orderRepo.findByIdAndClientId(orderId, 7L)).thenReturn(Optional.of(order));

        OrderEntity result = service.getClientOrderOrThrow(orderId, 7L);

        assertSame(order, result);
    }

    @Test
    void getClientOrderOrThrow_throwsNotFoundWhenOrderMissingOrForeign() {
        UUID orderId = UUID.randomUUID();
        when(orderRepo.findByIdAndClientId(orderId, 7L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getClientOrderOrThrow(orderId, 7L)
        );

        assertEquals(404, exception.getStatusCode().value());
    }
}
