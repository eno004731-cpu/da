package order_service.services.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.fasterxml.jackson.databind.ObjectMapper;

import order_service.persistence.events.outbox.OutboxEventEntity;
import order_service.persistence.events.outbox.OutboxEventRepo;
import order_service.persistence.order.OrderEntity;
import order_service.services.events.EventStatusService;
import order_service.services.events.OutboxWakeUpEvent;

@ExtendWith(MockitoExtension.class)
class ServiceNameOutboxServiceTest {
    @Mock
    private EventStatusService eventStatusService;

    @Mock
    private OutboxEventRepo eventRepo;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ServiceNameOutboxService service;

    @Test
    void createServiceNameRequestedEvent_savesOutboxEventAndPublishesWakeUp() {
        UUID orderId = UUID.randomUUID();
        OrderEntity order = new OrderEntity();
        order.setId(orderId);
        order.setServiceCode("CONSULT");

        service = new ServiceNameOutboxService(objectMapper, eventStatusService, eventRepo, applicationEventPublisher);
        service.createServiceNameRequestedEvent(order);

        ArgumentCaptor<OutboxEventEntity> eventCaptor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(eventRepo).save(eventCaptor.capture());
        OutboxEventEntity event = eventCaptor.getValue();
        assertNotNull(event.getId());
        assertEquals(orderId, event.getAggregateId());
        assertEquals("SERVICE_NAME_REQUESTED", event.getEventType());
        assertEquals("NEW", event.getStatus());
        assertEquals(0, event.getRetryCount());
        assertEquals(orderId, objectMapper.convertValue(event.getPayload().get("orderId"), UUID.class));
        assertEquals("CONSULT", event.getPayload().get("serviceCode").asText());
        assertEquals(event.getId(), objectMapper.convertValue(event.getPayload().get("eventId"), UUID.class));
        verify(applicationEventPublisher).publishEvent(any(OutboxWakeUpEvent.class));
    }
}
