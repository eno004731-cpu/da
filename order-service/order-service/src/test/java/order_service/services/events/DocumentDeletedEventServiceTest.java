package order_service.services.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import order_service.dto.payload.DocumentDeletedPayload;
import order_service.persistence.document.OrderDocumentMetadataEntity;
import order_service.persistence.document.OrderDocumentMetadataRepo;
import order_service.persistence.events.incoming.IncomingEventEntity;
import order_service.persistence.events.incoming.IncomingEventRepo;

@ExtendWith(MockitoExtension.class)
class DocumentDeletedEventServiceTest {
    @Mock
    private IncomingEventRepo incomingEventRepo;

    @Mock
    private OrderDocumentMetadataRepo documentMetadataRepo;

    @Mock
    private EventStatusService eventStatusService;

    @Test
    void handleDocumentDeleted_marksMetadataDeletedAndProcessesEvent() {
        DocumentDeletedEventService service = createService();
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        DocumentDeletedPayload payload = payload(eventId, orderId, "doc-1");
        OrderDocumentMetadataEntity metadata = new OrderDocumentMetadataEntity();
        metadata.setDocumentId("doc-1");
        metadata.setOrderId(orderId);
        metadata.setIsDeleted(false);

        when(incomingEventRepo.existsByEventId(eventId)).thenReturn(false);
        when(incomingEventRepo.save(any(IncomingEventEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(documentMetadataRepo.findByOrderIdAndDocumentId(orderId, "doc-1")).thenReturn(Optional.of(metadata));

        service.handleDocumentDeleted(record(payload));

        ArgumentCaptor<OrderDocumentMetadataEntity> metadataCaptor = ArgumentCaptor.forClass(OrderDocumentMetadataEntity.class);
        verify(documentMetadataRepo).save(metadataCaptor.capture());
        assertEquals(true, metadataCaptor.getValue().getIsDeleted());
        assertEquals(LocalDateTime.parse("2026-06-14T10:00:00"), metadataCaptor.getValue().getDeletedAt());
        verify(eventStatusService).saveProcessedIncomingEvent(any(IncomingEventEntity.class));
    }

    @Test
    void handleDocumentDeleted_skipsAlreadyProcessedEvent() {
        DocumentDeletedEventService service = createService();
        UUID eventId = UUID.randomUUID();
        DocumentDeletedPayload payload = payload(eventId, UUID.randomUUID(), "doc-1");

        when(incomingEventRepo.existsByEventId(eventId)).thenReturn(true);

        service.handleDocumentDeleted(record(payload));

        verify(documentMetadataRepo, never()).save(any());
    }

    private ConsumerRecord<String, DocumentDeletedPayload> record(DocumentDeletedPayload payload) {
        return new ConsumerRecord<>("document.deleted", 0, 15L, payload.getOrderId().toString(), payload);
    }

    private DocumentDeletedPayload payload(UUID eventId, UUID orderId, String documentId) {
        DocumentDeletedPayload payload = new DocumentDeletedPayload();
        payload.setEventId(eventId);
        payload.setOrderId(orderId);
        payload.setDocumentId(documentId);
        payload.setDeletedAt("2026-06-14T10:00:00");
        payload.setIsDeleted(true);
        return payload;
    }

    private DocumentDeletedEventService createService() {
        DocumentDeletedEventService service = new DocumentDeletedEventService(
                incomingEventRepo,
                documentMetadataRepo,
                new ObjectMapper().findAndRegisterModules(),
                eventStatusService
        );
        ReflectionTestUtils.setField(service, "consumerGroup", "order-service");
        return service;
    }
}
