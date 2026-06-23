package order_service.services.orders;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import order_service.dto.response.ClientOrderDetailsResponse;
import order_service.persistence.order.OrderEntity;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientOrderDetailsServiceTest {

    @Mock
    private ClientOrderAccessService clientOrderAccessService;

    @Spy
    private OrderResponseMapper orderResponseMapper = new OrderResponseMapper();

    @InjectMocks
    private ClientOrderDetailsService service;

    @Test
    void getOrderDetails_returnsMappedOrder() {
        UUID orderId = UUID.randomUUID();
        OrderEntity order = new OrderEntity();
        order.setId(orderId);
        order.setTitle("Consultation");
        order.setServiceCode("CONSULT");
        order.setServiceName("Юридическая консультация");
        order.setClientName("Nikita");
        order.setContact("+79990000000");
        order.setCompanyName("Acme");
        order.setProblemDescription("Need help");
        order.setStatus("ON_REVIEW");
        order.setCreateAt(LocalDateTime.now().minusHours(1));
        order.setUpdatedAt(LocalDateTime.now());
        when(clientOrderAccessService.getClientOrderOrThrow(orderId, 7L)).thenReturn(order);

        ClientOrderDetailsResponse response = service.getOrderDetails(orderId, 7L);

        assertEquals(orderId, response.getId());
        assertEquals("Consultation", response.getTitle());
        assertEquals("CONSULT", response.getServiceCode());
        assertEquals("Юридическая консультация", response.getServiceName());
    }

    @Test
    void getOrderDetails_throwsNotFoundWhenOrderBelongsToAnotherClient() {
        UUID orderId = UUID.randomUUID();
        when(clientOrderAccessService.getClientOrderOrThrow(orderId, 7L))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Заказ не найден"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> service.getOrderDetails(orderId, 7L));

        assertEquals(404, exception.getStatusCode().value());
    }

    @Test
    void getOrderDetails_throwsNotFoundWhenOrderMissing() {
        UUID orderId = UUID.randomUUID();
        when(clientOrderAccessService.getClientOrderOrThrow(orderId, 7L))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Заказ не найден"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> service.getOrderDetails(orderId, 7L));

        assertEquals(404, exception.getStatusCode().value());
    }
}
