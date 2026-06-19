package order_service.services.catalog;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import order_service.dto.payload.GetServiceNamePayload;
import order_service.persistence.events.outbox.OutboxEventEntity;
import order_service.persistence.events.outbox.OutboxEventRepo;
import order_service.persistence.order.OrderEntity;
import order_service.persistence.order.OrderRepo;
import order_service.services.events.outbox.EventStatusService;

@ExtendWith(MockitoExtension.class)
class SendEventForGetServiceNameTest {

    @Mock
    private OutboxEventRepo eventRepo;

    @Mock
    private KafkaTemplate<String, GetServiceNamePayload> kafkaTemplateCatalog;

    @Mock
    private EventStatusService eventStatusService;

    @Mock
    private OrderRepo orderRepo;

    @InjectMocks
    private SendEventForGetServiceName service;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "objectmapper", objectMapper);
        ReflectionTestUtils.setField(service, "requestTopic", "order.getServiceName.request");
    }

    @Test
    void sendEvent_processesNewAndRetryableFailedEvents() {
        OutboxEventEntity newEvent = createEvent("CONSULT");
        OutboxEventEntity failedEvent = createEvent("DOCUMENT");
        when(eventRepo.findTop100ByStatusOrderByCreatedAtAsc("NEW")).thenReturn(List.of(newEvent));
        when(eventRepo.findTop100ByStatusAndNextRetryAtBeforeOrderByNextRetryAtAsc(eq("FAILED"), any(LocalDateTime.class)))
                .thenReturn(List.of(failedEvent));
        when(orderRepo.findById(any(UUID.class))).thenReturn(Optional.of(new OrderEntity()));
        when(kafkaTemplateCatalog.send(anyString(), anyString(), any(GetServiceNamePayload.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        service.sendEvent();

        verify(orderRepo, times(2)).findById(any(UUID.class));
        verify(kafkaTemplateCatalog, times(2)).send(eq("order.getServiceName.request"), anyString(), any(GetServiceNamePayload.class));
        verify(eventStatusService, times(2)).savePublishedEvent(any(OutboxEventEntity.class));
    }

    @Test
    void sendEvent_marksEventDeadWhenServiceCodeIsBlank() {
        OutboxEventEntity event = createEvent(" ");
        when(eventRepo.findTop100ByStatusOrderByCreatedAtAsc("NEW")).thenReturn(List.of(event));
        when(eventRepo.findTop100ByStatusAndNextRetryAtBeforeOrderByNextRetryAtAsc(eq("FAILED"), any(LocalDateTime.class)))
                .thenReturn(List.of());

        service.sendEvent();

        verify(eventStatusService).saveDeadEvent(same(event), anyString());
        verify(orderRepo, never()).findById(any(UUID.class));
        verify(kafkaTemplateCatalog, never()).send(anyString(), anyString(), any(GetServiceNamePayload.class));
    }

    @Test
    void sendEvent_marksEventDeadWhenOrderDoesNotExist() {
        OutboxEventEntity event = createEvent("CONSULT");
        when(eventRepo.findTop100ByStatusOrderByCreatedAtAsc("NEW")).thenReturn(List.of(event));
        when(eventRepo.findTop100ByStatusAndNextRetryAtBeforeOrderByNextRetryAtAsc(eq("FAILED"), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(orderRepo.findById(event.getAggregateId())).thenReturn(Optional.empty());

        service.sendEvent();

        verify(eventStatusService).saveDeadEvent(same(event), anyString());
        verify(kafkaTemplateCatalog, never()).send(anyString(), anyString(), any(GetServiceNamePayload.class));
    }

    @Test
    void sendEvent_marksEventFailedWhenPayloadDeserializationBreaks() throws Exception {
        ObjectMapper brokenMapper = org.mockito.Mockito.mock(ObjectMapper.class);
        ReflectionTestUtils.setField(service, "objectmapper", brokenMapper);
        OutboxEventEntity event = createEvent("CONSULT");
        when(eventRepo.findTop100ByStatusOrderByCreatedAtAsc("NEW")).thenReturn(List.of(event));
        when(eventRepo.findTop100ByStatusAndNextRetryAtBeforeOrderByNextRetryAtAsc(eq("FAILED"), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(brokenMapper.treeToValue(any(JsonNode.class), eq(GetServiceNamePayload.class)))
                .thenThrow(new RuntimeException("bad payload"));

        service.sendEvent();

        verify(eventStatusService).saveFailedEvent(same(event), anyString());
        verify(orderRepo, never()).findById(any(UUID.class));
    }

    @Test
    void sendEvent_marksEventPublishedWhenKafkaSendSucceeds() {
        OutboxEventEntity event = createEvent("CONSULT");
        when(eventRepo.findTop100ByStatusOrderByCreatedAtAsc("NEW")).thenReturn(List.of(event));
        when(eventRepo.findTop100ByStatusAndNextRetryAtBeforeOrderByNextRetryAtAsc(eq("FAILED"), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(orderRepo.findById(event.getAggregateId())).thenReturn(Optional.of(new OrderEntity()));
        CompletableFuture<SendResult<String, GetServiceNamePayload>> successFuture = CompletableFuture.completedFuture(null);
        when(kafkaTemplateCatalog.send(anyString(), anyString(), any(GetServiceNamePayload.class))).thenReturn(successFuture);

        service.sendEvent();

        verify(eventStatusService).savePublishedEvent(same(event));
    }

    @Test
    void sendEvent_marksEventFailedWhenKafkaSendFails() {
        OutboxEventEntity event = createEvent("CONSULT");
        when(eventRepo.findTop100ByStatusOrderByCreatedAtAsc("NEW")).thenReturn(List.of(event));
        when(eventRepo.findTop100ByStatusAndNextRetryAtBeforeOrderByNextRetryAtAsc(eq("FAILED"), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(orderRepo.findById(event.getAggregateId())).thenReturn(Optional.of(new OrderEntity()));
        CompletableFuture<SendResult<String, GetServiceNamePayload>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("kafka down"));
        when(kafkaTemplateCatalog.send(anyString(), anyString(), any(GetServiceNamePayload.class))).thenReturn(failedFuture);

        service.sendEvent();

        verify(eventStatusService).saveFailedEvent(same(event), anyString());
    }

    private OutboxEventEntity createEvent(String serviceCode) {
        UUID orderId = UUID.randomUUID();
        GetServiceNamePayload payload = new GetServiceNamePayload();
        payload.setEventId(UUID.randomUUID());
        payload.setOrderId(orderId);
        payload.setServiceCode(serviceCode);

        OutboxEventEntity event = new OutboxEventEntity();
        event.setId(UUID.randomUUID());
        event.setAggregateId(orderId);
        event.setStatus("NEW");
        event.setRetryCount(0);
        event.setCreatedAt(LocalDateTime.now());
        event.setPayload(objectMapper.valueToTree(payload));
        return event;
    }
}
