package order_service.integration;

import order_service.Dto.payload.DocumentStoredPayload;
import order_service.EntityAndRepo.document.OrderDocumentMetadataEntity;
import order_service.EntityAndRepo.document.OrderDocumentMetadataRepo;
import order_service.EntityAndRepo.events.incoming.IncomingEventEntity;
import order_service.EntityAndRepo.events.incoming.IncomingEventRepo;
import order_service.EntityAndRepo.order.OrderEntity;
import order_service.EntityAndRepo.order.OrderRepo;
import order_service.Services.events.DocumentStoredEventService;
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
    private OrderDocumentMetadataRepo documentMetadataRepo;

    @Autowired
    private IncomingEventRepo incomingEventRepo;

    @Test
    void handleDocumentStored_savesIncomingEventAndDocumentMetadataOnce() {
        OrderEntity order = orderRepo.save(orderForClient(7L));
        UUID eventId = UUID.randomUUID();
        DocumentStoredPayload payload = payload(eventId, order.getId(), "document-101");

        documentStoredEventService.handleDocumentStored(record(payload, 15L));
        documentStoredEventService.handleDocumentStored(record(payload, 16L));

        assertThat(incomingEventRepo.findAll()).hasSize(1);
        IncomingEventEntity incomingEvent = incomingEventRepo.findByEventId(eventId).orElseThrow();
        assertThat(incomingEvent.getStatus()).isEqualTo("PROCESSED");
        assertThat(incomingEvent.getAggregateId()).isEqualTo(order.getId());

        assertThat(documentMetadataRepo.findAll()).hasSize(1);
        OrderDocumentMetadataEntity metadata = documentMetadataRepo.findByDocumentId("document-101").orElseThrow();
        assertThat(metadata.getOrderId()).isEqualTo(order.getId());
        assertThat(metadata.getUploadedByUserId()).isEqualTo(7L);
        assertThat(metadata.getFileName()).isEqualTo("contract.pdf");
        assertThat(metadata.getMetadata().get("eventId").asText()).isEqualTo(eventId.toString());
    }

    @Test
    void handleDocumentStored_marksEventDeadWhenOrderDoesNotExist() {
        UUID missingOrderId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        DocumentStoredPayload payload = payload(eventId, missingOrderId, "document-404");

        documentStoredEventService.handleDocumentStored(record(payload, 20L));

        IncomingEventEntity incomingEvent = incomingEventRepo.findByEventId(eventId).orElseThrow();
        assertThat(incomingEvent.getStatus()).isEqualTo("DEAD");
        assertThat(incomingEvent.getLastError()).contains("Заказ для document.stored не найден");
        assertThat(documentMetadataRepo.findByDocumentId("document-404")).isEmpty();
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
