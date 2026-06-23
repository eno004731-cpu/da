package order_service.services.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import order_service.dto.payload.GetServiceNamePayload;
import order_service.persistence.events.incoming.IncomingEventEntity;
import order_service.persistence.events.incoming.IncomingEventRepo;
import order_service.persistence.order.OrderEntity;
import order_service.persistence.order.OrderRepo;
import order_service.services.events.outbox.EventStatusService;

@ExtendWith(MockitoExtension.class)
class ListenCatalogServiceTest {

    @Mock
    private IncomingEventRepo incomingEventRepo;

    @Mock
    private OrderRepo orderRepo;

    @Mock
    private EventStatusService eventStatusService;

    @InjectMocks
    private ListenCatalogService service;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "consumerGroup", "order-service-tests");
        ReflectionTestUtils.setField(service, "objectMapper", objectMapper);
    }

    @Test
    void handleCatalogResponse_updatesOrderAndMarksIncomingEventProcessed() {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        GetServiceNamePayload payload = new GetServiceNamePayload();
        payload.setEventId(eventId);
        payload.setOrderId(orderId);
        payload.setServiceCode("CONSULT");
        payload.setServiceName("Consultation");
        ConsumerRecord<String, GetServiceNamePayload> record =
                new ConsumerRecord<>("catalog.response", 1, 10L, orderId.toString(), payload);
        OrderEntity order = new OrderEntity();
        order.setId(orderId);

        when(incomingEventRepo.existsByEventId(eventId)).thenReturn(false);
        when(orderRepo.findById(orderId)).thenReturn(Optional.of(order));

        service.handleCatalogResponse(record);

        ArgumentCaptor<IncomingEventEntity> incomingCaptor = ArgumentCaptor.forClass(IncomingEventEntity.class);
        verify(incomingEventRepo).save(incomingCaptor.capture());
        IncomingEventEntity savedIncomingEvent = incomingCaptor.getValue();
        assertEquals(eventId, savedIncomingEvent.getEventId());
        assertEquals(orderId, savedIncomingEvent.getAggregateId());
        assertEquals("SERVICE_NAME_RESOLVED", savedIncomingEvent.getEventType());
        // Status хранится как enum, поэтому тест проверяет контракт доменной модели,
        // а не строковое представление значения.
        assertEquals(IncomingEventEntity.Status.RECEIVED, savedIncomingEvent.getStatus());
        assertEquals("order-service-tests", savedIncomingEvent.getConsumerGroup());
        assertNotNull(savedIncomingEvent.getReceivedAt());

        assertEquals("Consultation", order.getServiceName());
        assertNotNull(order.getUpdatedAt());
        verify(orderRepo).save(same(order));
        verify(eventStatusService).saveProcessedIncomingEvent(any(IncomingEventEntity.class));
        verify(eventStatusService, never()).saveDeadIncomingEvent(any(IncomingEventEntity.class), any());
    }

    @Test
    void handleCatalogResponse_skipsAlreadyProcessedEvent() {
        UUID eventId = UUID.randomUUID();
        GetServiceNamePayload payload = new GetServiceNamePayload();
        payload.setEventId(eventId);
        ConsumerRecord<String, GetServiceNamePayload> record =
                new ConsumerRecord<>("catalog.response", 1, 10L, "key", payload);

        when(incomingEventRepo.existsByEventId(eventId)).thenReturn(true);

        service.handleCatalogResponse(record);

        verify(incomingEventRepo, never()).save(any(IncomingEventEntity.class));
        verify(orderRepo, never()).findById(any());
        verify(eventStatusService, never()).saveProcessedIncomingEvent(any(IncomingEventEntity.class));
        verify(eventStatusService, never()).saveDeadIncomingEvent(any(IncomingEventEntity.class), any());
    }

    @Test
    void handleCatalogResponse_marksIncomingEventDeadWhenOrderIdIsMissing() {
        UUID eventId = UUID.randomUUID();
        GetServiceNamePayload payload = new GetServiceNamePayload();
        payload.setEventId(eventId);
        payload.setServiceName("Consultation");
        ConsumerRecord<String, GetServiceNamePayload> record =
                new ConsumerRecord<>("catalog.response", 1, 10L, "key", payload);

        when(incomingEventRepo.existsByEventId(eventId)).thenReturn(false);

        service.handleCatalogResponse(record);

        verify(incomingEventRepo).save(any(IncomingEventEntity.class));
        verify(eventStatusService).saveDeadIncomingEvent(any(IncomingEventEntity.class), any());
        verify(orderRepo, never()).findById(any());
    }

    @Test
    void handleCatalogResponse_marksIncomingEventDeadWhenServiceNameIsBlank() {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        GetServiceNamePayload payload = new GetServiceNamePayload();
        payload.setEventId(eventId);
        payload.setOrderId(orderId);
        payload.setServiceName(" ");
        ConsumerRecord<String, GetServiceNamePayload> record =
                new ConsumerRecord<>("catalog.response", 1, 10L, orderId.toString(), payload);

        when(incomingEventRepo.existsByEventId(eventId)).thenReturn(false);

        service.handleCatalogResponse(record);

        verify(incomingEventRepo).save(any(IncomingEventEntity.class));
        verify(eventStatusService).saveDeadIncomingEvent(any(IncomingEventEntity.class), any());
        verify(orderRepo, never()).findById(any());
    }

    @Test
    void handleCatalogResponse_marksIncomingEventDeadWhenOrderNotFound() {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        GetServiceNamePayload payload = new GetServiceNamePayload();
        payload.setEventId(eventId);
        payload.setOrderId(orderId);
        payload.setServiceName("Consultation");
        ConsumerRecord<String, GetServiceNamePayload> record =
                new ConsumerRecord<>("catalog.response", 1, 10L, orderId.toString(), payload);

        when(incomingEventRepo.existsByEventId(eventId)).thenReturn(false);
        when(orderRepo.findById(orderId)).thenReturn(Optional.empty());

        service.handleCatalogResponse(record);

        verify(incomingEventRepo).save(any(IncomingEventEntity.class));
        verify(orderRepo).findById(orderId);
        verify(eventStatusService).saveDeadIncomingEvent(any(IncomingEventEntity.class), any());
        verify(orderRepo, never()).save(any(OrderEntity.class));
    }
}
