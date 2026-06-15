package order_service.services.orders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import order_service.dto.request.UpdateClientOrderRequest;
import order_service.dto.response.ClientOrderDetailsResponse;
import order_service.persistence.order.OrderEntity;
import order_service.persistence.order.OrderRepo;

@ExtendWith(MockitoExtension.class)
class ClientOrderUpdateServiceTest {
    @Mock
    private ClientOrderAccessService clientOrderAccessService;

    @Mock
    private ClientOrderDetailsService clientOrderDetailsService;

    @Mock
    private OrderRepo orderRepo;

    @InjectMocks
    private ClientOrderUpdateService service;

    @Test
    void updateOrder_updatesAllowedFieldsAndReturnsDetails() {
        UUID orderId = UUID.randomUUID();
        OrderEntity order = new OrderEntity();
        UpdateClientOrderRequest request = new UpdateClientOrderRequest();
        request.setServiceCode(" CONTRACT ");
        request.setClientName(" Client ");
        request.setContact(" +79990000000 ");
        request.setCompanyName(" Acme ");
        request.setDescription(" Update contract ");

        ClientOrderDetailsResponse response = new ClientOrderDetailsResponse();
        response.setId(orderId);
        response.setDocuments(List.of());

        when(clientOrderAccessService.getClientOrderOrThrow(orderId, 7L)).thenReturn(order);
        when(clientOrderDetailsService.getOrderDetails(orderId, 7L)).thenReturn(response);

        ClientOrderDetailsResponse result = service.updateOrder(orderId, 7L, request);

        ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepo).save(orderCaptor.capture());
        OrderEntity savedOrder = orderCaptor.getValue();
        assertEquals("CONTRACT", savedOrder.getServiceCode());
        assertEquals("Client", savedOrder.getClientName());
        assertEquals("+79990000000", savedOrder.getContact());
        assertEquals("Acme", savedOrder.getCompanyName());
        assertEquals("Update contract", savedOrder.getProblemDescription());
        assertEquals("Update contract", savedOrder.getTitle());
        assertEquals(response, result);
    }
}
