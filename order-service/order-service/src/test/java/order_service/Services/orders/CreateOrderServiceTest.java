package order_service.Services.orders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import org.springframework.context.ApplicationEventPublisher;

import com.fasterxml.jackson.databind.ObjectMapper;

import order_service.Dto.request.CreateOrderRequest;
import order_service.Dto.response.CreateOrderResponse;
import order_service.EntityAndRepo.events.outbox.OutboxEventEntity;
import order_service.EntityAndRepo.events.outbox.OutboxEventRepo;
import order_service.EntityAndRepo.order.OrderEntity;
import order_service.EntityAndRepo.order.OrderRepo;
import order_service.Services.events.EventStatusService;
import order_service.Services.events.OutboxWakeUpEvent;

@ExtendWith(MockitoExtension.class)
class CreateOrderServiceTest {

    @Mock
    private OrderRepo orderRepo;

    @Mock
    private EventStatusService eventStatusService;

    @Mock
    private OutboxEventRepo eventRepo;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private CreateOrderService service;

    private final ObjectMapper objectMapper = new ObjectMapper();

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

        service = new CreateOrderService(orderRepo, objectMapper, eventStatusService, eventRepo, applicationEventPublisher);
        CreateOrderResponse response = service.createOrder(request, 42L);

        ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepo).save(orderCaptor.capture());
        OrderEntity savedOrder = orderCaptor.getValue();
        assertEquals(42L, savedOrder.getClientId());
        assertEquals("CONSULT", savedOrder.getServiceCode());
        assertEquals("ON_REVIEW", savedOrder.getStatus());
        assertEquals("Need legal advice", savedOrder.getTitle());
        assertEquals("Need legal advice", savedOrder.getProblemDescription());
        assertNotNull(savedOrder.getCreateAt());
        assertNotNull(savedOrder.getUpdatedAt());

        ArgumentCaptor<OutboxEventEntity> eventCaptor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(eventRepo).save(eventCaptor.capture());
        OutboxEventEntity savedEvent = eventCaptor.getValue();
        assertNotNull(savedEvent.getId());
        assertEquals(orderId, savedEvent.getAggregateId());
        assertEquals("SERVICE_NAME_REQUESTED", savedEvent.getEventType());
        assertEquals("NEW", savedEvent.getStatus());
        assertEquals(0, savedEvent.getRetryCount());
        assertEquals(orderId, objectMapper.convertValue(savedEvent.getPayload().get("orderId"), UUID.class));
        assertEquals("CONSULT", savedEvent.getPayload().get("serviceCode").asText());
        assertEquals(savedEvent.getId(), objectMapper.convertValue(savedEvent.getPayload().get("eventId"), UUID.class));

        assertNotNull(response);
        assertEquals(orderId, response.getId());
        assertEquals(orderId, response.getOrderId());
        assertEquals("ON_REVIEW", response.getStatus());
        verify(applicationEventPublisher).publishEvent(any(OutboxWakeUpEvent.class));
        verify(orderRepo, times(1)).save(any(OrderEntity.class));
    }
}
