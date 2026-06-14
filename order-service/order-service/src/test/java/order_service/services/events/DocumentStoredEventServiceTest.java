package order_service.services.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import order_service.dto.payload.DocumentStoredPayload;
import order_service.persistence.document.OrderDocumentMetadataEntity;
import order_service.persistence.document.OrderDocumentMetadataRepo;
import order_service.persistence.events.incoming.IncomingEventEntity;
import order_service.persistence.events.incoming.IncomingEventRepo;
import order_service.persistence.order.OrderRepo;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentStoredEventServiceTest {
    @Mock
    private IncomingEventRepo incomingEventRepo;

    @Mock
    private OrderDocumentMetadataRepo documentMetadataRepo;

    @Mock
    private OrderRepo orderRepo;

    @Mock
    private EventStatusService eventStatusService;

    @Test
    void handleDocumentStored_savesMetadataAndMarksEventProcessed() {
        DocumentStoredEventService service = createService();
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        DocumentStoredPayload payload = payload(eventId, orderId);
        ConsumerRecord<String, DocumentStoredPayload> record =
                new ConsumerRecord<>("document.stored", 0, 15L, orderId.toString(), payload);

        when(incomingEventRepo.existsByEventId(eventId)).thenReturn(false);
        when(orderRepo.existsById(orderId)).thenReturn(true);
        when(documentMetadataRepo.existsByDocumentId("101")).thenReturn(false);
        when(incomingEventRepo.save(any(IncomingEventEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.handleDocumentStored(record);

        ArgumentCaptor<OrderDocumentMetadataEntity> metadataCaptor = ArgumentCaptor.forClass(OrderDocumentMetadataEntity.class);
        verify(documentMetadataRepo).save(metadataCaptor.capture());
        assertEquals("101", metadataCaptor.getValue().getDocumentId());
        assertEquals(orderId, metadataCaptor.getValue().getOrderId());
        verify(eventStatusService).saveProcessedIncomingEvent(any(IncomingEventEntity.class));
    }

    @Test
    void handleDocumentStored_skipsAlreadyProcessedEvent() {
        DocumentStoredEventService service = createService();
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        DocumentStoredPayload payload = payload(eventId, orderId);
        ConsumerRecord<String, DocumentStoredPayload> record =
                new ConsumerRecord<>("document.stored", 0, 15L, orderId.toString(), payload);

        when(incomingEventRepo.existsByEventId(eventId)).thenReturn(true);

        service.handleDocumentStored(record);

        verify(documentMetadataRepo, never()).save(any());
    }

    private DocumentStoredPayload payload(UUID eventId, UUID orderId) {
        DocumentStoredPayload payload = new DocumentStoredPayload();
        payload.setEventId(eventId);
        payload.setDocumentId("101");
        payload.setOrderId(orderId);
        payload.setUploadedByUserId(7L);
        payload.setFileName("contract.pdf");
        payload.setMimeType("application/pdf");
        payload.setSizeBytes(123L);
        payload.setUploadedAt(LocalDateTime.now().toString());
        payload.setIsDeleted(false);
        return payload;
    }

    private DocumentStoredEventService createService() {
        DocumentStoredEventService service = new DocumentStoredEventService(
                incomingEventRepo,
                documentMetadataRepo,
                orderRepo,
                new ObjectMapper().findAndRegisterModules(),
                eventStatusService
        );
        ReflectionTestUtils.setField(service, "consumerGroup", "order-service");
        return service;
    }
}
