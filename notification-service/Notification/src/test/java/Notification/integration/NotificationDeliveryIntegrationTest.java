package Notification.integration;

import Notification.Dto.VerityEmailPayload;
import Notification.EntityAndRepo.Events.EventEntity;
import Notification.EntityAndRepo.Nofilication.NofilicationEntity;
import Notification.Services.SendEmail;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationDeliveryIntegrationTest extends PostgresNotificationIntegrationTestBase {

    @Autowired
    SendEmail sendEmail;

    @Test
    void saveMessageStoresDeliveryAndProcessedEventOnce() {
        UUID eventId = UUID.randomUUID();
        VerityEmailPayload payload = payload(eventId);
        ConsumerRecord<String, VerityEmailPayload> record =
                new ConsumerRecord<>("auth.email-verification.requested", 1, 42L, "100", payload);

        sendEmail.saveMesssage(record);
        sendEmail.saveMesssage(record);

        assertThat(nofilicationRepo.findAll()).hasSize(1);
        NofilicationEntity delivery = nofilicationRepo.findByEventId(eventId).orElseThrow();
        assertThat(delivery.getChannel()).isEqualTo("EMAIL");
        assertThat(delivery.getTemplateCode()).isEqualTo("EMAIL_VERIFY_LINK");
        assertThat(delivery.getRecipient()).isEqualTo(payload.getEmail());
        assertThat(delivery.getSubject()).isEqualTo("Подтверждение email");
        assertThat(delivery.getStatus()).isEqualTo("NEW");
        assertThat(delivery.getPayload().get("eventId").asText()).isEqualTo(eventId.toString());
        assertThat(delivery.getPayload().get("link").asText()).isEqualTo(payload.getLink());

        assertThat(eventRepo.findAll()).hasSize(1);
        EventEntity processedEvent = eventRepo.findByEventId(eventId).orElseThrow();
        assertThat(processedEvent.getTopic()).isEqualTo("auth.email-verification.requested");
        assertThat(processedEvent.getPartitionNo()).isEqualTo(1);
        assertThat(processedEvent.getMessageOffset()).isEqualTo(42L);
        assertThat(processedEvent.getConsumerGroup()).isEqualTo("notification-service");
        assertThat(processedEvent.getStatus()).isEqualTo("ACCEPTED");
    }

    @Test
    void sendMessageMarksNewDeliveryAsSentWithoutRealSmtp() {
        UUID eventId = UUID.randomUUID();
        ConsumerRecord<String, VerityEmailPayload> record =
                new ConsumerRecord<>("auth.email-verification.requested", 0, 7L, "100", payload(eventId));

        sendEmail.saveMesssage(record);
        sendEmail.sendMessage();

        NofilicationEntity delivery = nofilicationRepo.findByEventId(eventId).orElseThrow();
        assertThat(delivery.getStatus()).isEqualTo("SENT");
        assertThat(delivery.getSentAt()).isNotNull();
        assertThat(delivery.getLastError()).isNull();
        assertThat(delivery.getNextRetryAt()).isNull();
    }

    private VerityEmailPayload payload(UUID eventId) {
        VerityEmailPayload payload = new VerityEmailPayload();
        payload.setEventId(eventId);
        payload.setUserId(100L);
        payload.setEmail("client-" + eventId + "@test.local");
        payload.setVerificationCodeId(200L);
        payload.setPurpose("VERIFY_EMAIL");
        payload.setChannel("EMAIL");
        payload.setLink("http://127.0.0.1:8000/verify-email?token=test-token");
        return payload;
    }
}
