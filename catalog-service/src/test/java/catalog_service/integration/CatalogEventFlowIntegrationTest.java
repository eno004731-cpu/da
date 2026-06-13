package catalog_service.integration;

import catalog_service.dto.payload.GetServiceNamePayload;
import catalog_service.entityAndRepo.inbox.InboxEventEntity;
import catalog_service.entityAndRepo.outbox.OutboxEventEntity;
import catalog_service.services.CreateOutboxEvent;
import catalog_service.services.ListenKafkaService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogEventFlowIntegrationTest extends PostgresCatalogIntegrationTestBase {

    @Autowired
    ListenKafkaService listenKafkaService;

    @Autowired
    CreateOutboxEvent createOutboxEvent;

    @Test
    void saveEventAndCreateOutboxEventStoresEnrichedResponseOnce() {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        GetServiceNamePayload payload = payload(eventId, orderId, "REGISTRATION");

        // Так мы проверяем consumer-логику без настоящего Kafka broker: сервису важен payload record'а.
        ConsumerRecord<String, GetServiceNamePayload> record =
                new ConsumerRecord<>("catalog.get-service-name.request", 0, 0, orderId.toString(), payload);

        listenKafkaService.saveEvent(record);
        listenKafkaService.saveEvent(record);
        createOutboxEvent.createOutboxEvent();

        assertThat(inboxEventRepo.findAll()).hasSize(1);
        InboxEventEntity inboxEvent = inboxEventRepo.findByEventId(eventId).orElseThrow();
        assertThat(inboxEvent.getStatus()).isEqualTo("PROCESSED");
        assertThat(inboxEvent.getProcessedAt()).isNotNull();

        assertThat(outboxEventRepo.findAll()).hasSize(1);
        OutboxEventEntity outboxEvent = outboxEventRepo.findAll().get(0);
        assertThat(outboxEvent.getAggregateId()).isEqualTo(orderId);
        assertThat(outboxEvent.getEventType()).isEqualTo("GetServiceName");
        assertThat(outboxEvent.getStatus()).isEqualTo("NEW");
        assertThat(outboxEvent.getPayload().get("eventId").asText()).isEqualTo(eventId.toString());
        assertThat(outboxEvent.getPayload().get("orderId").asText()).isEqualTo(orderId.toString());
        assertThat(outboxEvent.getPayload().get("serviceCode").asText()).isEqualTo("REGISTRATION");
        assertThat(outboxEvent.getPayload().get("serviceName").asText()).isEqualTo("Регистрация ООО / ИП");
    }

    @Test
    void unknownServiceCodeMovesInboxEventToDeadWithoutOutbox() {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        GetServiceNamePayload payload = payload(eventId, orderId, "UNKNOWN_SERVICE");
        ConsumerRecord<String, GetServiceNamePayload> record =
                new ConsumerRecord<>("catalog.get-service-name.request", 0, 0, orderId.toString(), payload);

        listenKafkaService.saveEvent(record);
        createOutboxEvent.createOutboxEvent();

        InboxEventEntity inboxEvent = inboxEventRepo.findByEventId(eventId).orElseThrow();
        assertThat(inboxEvent.getStatus()).isEqualTo("DEAD");
        assertThat(inboxEvent.getLastError()).isEqualTo("Not found service");
        assertThat(outboxEventRepo.findAll()).isEmpty();
    }

    private GetServiceNamePayload payload(UUID eventId, UUID orderId, String serviceCode) {
        GetServiceNamePayload payload = new GetServiceNamePayload();
        payload.setEventId(eventId);
        payload.setOrderId(orderId);
        payload.setServiceCode(serviceCode);
        return payload;
    }
}
