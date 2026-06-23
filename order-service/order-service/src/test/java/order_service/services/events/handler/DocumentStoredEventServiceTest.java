package order_service.services.events.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import order_service.dto.payload.DocumentStoredPayload;
import order_service.persistence.events.incoming.IncomingEventEntity;
import order_service.persistence.events.incoming.IncomingEventRepo;
import order_service.persistence.order.OrderRepo;
import order_service.services.events.outbox.EventStatusService;
import order_service.services.events.outbox.DocumentValidationOutboxService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentStoredEventServiceTest {
    @Mock
    private IncomingEventRepo incomingEventRepo;

    @Mock
    private OrderRepo orderRepo;

    @Mock
    private EventStatusService eventStatusService;

    @Mock
    private DocumentValidationOutboxService documentValidationOutboxService;

    @Test
    void handleDocumentStored_marksValidEventProcessed() {
        DocumentStoredEventService service = createService();
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        DocumentStoredPayload payload = payload(eventId, orderId);
        ConsumerRecord<String, DocumentStoredPayload> record =
                new ConsumerRecord<>("document.stored", 0, 15L, orderId.toString(), payload);

        when(incomingEventRepo.existsByEventId(eventId)).thenReturn(false);
        when(orderRepo.existsByIdAndClientIdAndIsDeletedFalseAndDeletionInProgressFalse(orderId, 7L))
                .thenReturn(true);
        when(incomingEventRepo.save(any(IncomingEventEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.handleDocumentStored(record);

        verify(eventStatusService).saveProcessedIncomingEvent(any(IncomingEventEntity.class));
        verify(documentValidationOutboxService).createSuccessfulValidationEvent(payload);
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

        verify(incomingEventRepo).existsByEventId(eventId);
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
                orderRepo,
                new ObjectMapper().findAndRegisterModules(),
                eventStatusService,
                documentValidationOutboxService
        );
        ReflectionTestUtils.setField(service, "consumerGroup", "order-service");
        return service;
    }
}
