package notification.integration;

import notification.dto.VerifyEmailPayload;
import notification.persistence.events.EventEntity;
import notification.persistence.notification.NotificationEntity;
import notification.services.SendEmail;
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
        VerifyEmailPayload payload = payload(eventId);
        ConsumerRecord<String, VerifyEmailPayload> record =
                new ConsumerRecord<>("auth.email-verification.requested", 1, 42L, "100", payload);

        sendEmail.saveMessage(record);
        sendEmail.saveMessage(record);

        assertThat(notificationRepo.findAll()).hasSize(1);
        NotificationEntity delivery = notificationRepo.findByEventId(eventId).orElseThrow();
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
        ConsumerRecord<String, VerifyEmailPayload> record =
                new ConsumerRecord<>("auth.email-verification.requested", 0, 7L, "100", payload(eventId));

        sendEmail.saveMessage(record);
        sendEmail.sendMessage();

        NotificationEntity delivery = notificationRepo.findByEventId(eventId).orElseThrow();
        assertThat(delivery.getStatus()).isEqualTo("SENT");
        assertThat(delivery.getSentAt()).isNotNull();
        assertThat(delivery.getLastError()).isNull();
        assertThat(delivery.getNextRetryAt()).isNull();
    }

    private VerifyEmailPayload payload(UUID eventId) {
        VerifyEmailPayload payload = new VerifyEmailPayload();
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
