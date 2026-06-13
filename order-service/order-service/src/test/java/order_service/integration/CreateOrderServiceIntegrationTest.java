package order_service.integration;

import order_service.Dto.request.CreateOrderRequest;
import order_service.Dto.response.CreateOrderResponse;
import order_service.EntityAndRepo.events.outbox.OutboxEventEntity;
import order_service.EntityAndRepo.events.outbox.OutboxEventRepo;
import order_service.EntityAndRepo.order.OrderEntity;
import order_service.EntityAndRepo.order.OrderRepo;
import order_service.Services.orders.CreateOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CreateOrderServiceIntegrationTest extends PostgresIntegrationTestBase {
    @Autowired
    private CreateOrderService createOrderService;

    @Autowired
    private OrderRepo orderRepo;

    @Autowired
    private OutboxEventRepo outboxEventRepo;

    @Test
    void createOrder_persistsOrderAndOutboxEventInPostgres() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setClientName("Nikita");
        request.setCompanyName("Acme");
        request.setContact("+79990000000");
        request.setServiceCode("CONSULT");
        request.setDescription("Need legal consultation");

        CreateOrderResponse response = createOrderService.createOrder(request, 42L);

        OrderEntity savedOrder = orderRepo.findById(response.getOrderId()).orElseThrow();
        assertThat(savedOrder.getClientId()).isEqualTo(42L);
        assertThat(savedOrder.getClientName()).isEqualTo("Nikita");
        assertThat(savedOrder.getServiceCode()).isEqualTo("CONSULT");
        assertThat(savedOrder.getStatus()).isEqualTo("ON_REVIEW");
        assertThat(savedOrder.getTitle()).isEqualTo("Need legal consultation");

        List<OutboxEventEntity> events = outboxEventRepo.findAll();
        assertThat(events).hasSize(1);
        OutboxEventEntity event = events.get(0);
        assertThat(event.getAggregateId()).isEqualTo(savedOrder.getId());
        assertThat(event.getEventType()).isEqualTo("SERVICE_NAME_REQUESTED");
        assertThat(event.getStatus()).isEqualTo("NEW");
        assertThat(event.getPayload().get("orderId").asText()).isEqualTo(savedOrder.getId().toString());
        assertThat(event.getPayload().get("serviceCode").asText()).isEqualTo("CONSULT");
    }
}
