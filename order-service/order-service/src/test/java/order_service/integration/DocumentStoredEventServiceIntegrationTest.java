package order_service.integration;

import order_service.dto.payload.DocumentStoredPayload;
import order_service.persistence.events.incoming.IncomingEventEntity;
import order_service.persistence.events.incoming.IncomingEventEntity.Status;
import order_service.persistence.events.incoming.IncomingEventRepo;
import order_service.persistence.events.outbox.OutboxEventEntity;
import order_service.persistence.events.outbox.OutboxEventRepo;
import order_service.persistence.order.OrderEntity;
import order_service.persistence.order.OrderRepo;
import order_service.services.events.handler.DocumentStoredEventService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentStoredEventServiceIntegrationTest extends PostgresIntegrationTestBase {
    @Autowired
    private DocumentStoredEventService documentStoredEventService;

    @Autowired
    private OrderRepo orderRepo;

    @Autowired
    private IncomingEventRepo incomingEventRepo;

    @Autowired
    private OutboxEventRepo outboxEventRepo;

    @Test
    void handleDocumentStored_savesIncomingEventOnce() {
        OrderEntity order = orderRepo.save(orderForClient(7L));
        UUID eventId = UUID.randomUUID();
        DocumentStoredPayload payload = payload(eventId, order.getId(), "101");

        documentStoredEventService.handleDocumentStored(record(payload, 15L));
        documentStoredEventService.handleDocumentStored(record(payload, 16L));

        assertThat(incomingEventRepo.findAll()).hasSize(1);
        IncomingEventEntity incomingEvent = incomingEventRepo.findByEventId(eventId).orElseThrow();
        assertThat(incomingEvent.getStatus()).isEqualTo(Status.PROCESSED);
        assertThat(incomingEvent.getAggregateId()).isEqualTo(order.getId());
        assertThat(outboxEventRepo.findAll()).hasSize(1);
        OutboxEventEntity outboxEvent = outboxEventRepo.findAll().get(0);
        assertThat(outboxEvent.getEventType()).isEqualTo("DOCUMENT_VALIDATION_RESULT");
        assertThat(outboxEvent.getStatus()).isEqualTo("NEW");
        assertThat(outboxEvent.getPayload().get("documentId").asLong()).isEqualTo(101L);
        assertThat(outboxEvent.getPayload().get("validationPassed").asBoolean()).isTrue();
    }

    @Test
    void handleDocumentStored_marksEventForDeletionWhenOrderDoesNotExist() {
        UUID missingOrderId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        DocumentStoredPayload payload = payload(eventId, missingOrderId, "404");

        documentStoredEventService.handleDocumentStored(record(payload, 20L));

        IncomingEventEntity incomingEvent = incomingEventRepo.findByEventId(eventId).orElseThrow();
        assertThat(incomingEvent.getStatus()).isEqualTo(Status.ON_DELETE);
        assertThat(incomingEvent.getLastError()).contains("Заказ для document.stored не найден");
        assertThat(outboxEventRepo.findAll()).isEmpty();
    }

    private ConsumerRecord<String, DocumentStoredPayload> record(DocumentStoredPayload payload, long offset) {
        return new ConsumerRecord<>("document.stored", 0, offset, payload.getOrderId().toString(), payload);
    }

    private DocumentStoredPayload payload(UUID eventId, UUID orderId, String documentId) {
        DocumentStoredPayload payload = new DocumentStoredPayload();
        payload.setEventId(eventId);
        payload.setDocumentId(documentId);
        payload.setOrderId(orderId);
        payload.setUploadedByUserId(7L);
        payload.setFileName("contract.pdf");
        payload.setMimeType("application/pdf");
        payload.setSizeBytes(2048L);
        payload.setUploadedAt(LocalDateTime.parse("2026-06-11T12:00:00").toString());
        payload.setIsDeleted(false);
        return payload;
    }

    private OrderEntity orderForClient(Long clientId) {
        LocalDateTime now = LocalDateTime.now();
        OrderEntity order = new OrderEntity();
        order.setClientId(clientId);
        order.setClientName("Client " + clientId);
        order.setContact("+79990000000");
        order.setCompanyName("Acme");
        order.setServiceCode("CONSULT");
        order.setTitle("Legal help");
        order.setProblemDescription("Need legal help");
        order.setStatus("ON_REVIEW");
        order.setCreateAt(now);
        order.setUpdatedAt(now);
        return order;
    }
}
