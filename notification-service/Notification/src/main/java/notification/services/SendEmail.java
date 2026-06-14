package notification.services;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import notification.dto.VerifyEmailPayload;
import notification.persistence.events.EventEntity;
import notification.persistence.events.EventRepo;
import notification.persistence.notification.NotificationEntity;
import notification.persistence.notification.NotificationRepo;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SendEmail {
    private final EventRepo eventRepo;
    private final NotificationRepo notificationRepo;
    private final ObjectMapper objectMapper;
    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    @Value("${app.notification-worker.processing-timeout-seconds:120}")
    private long processingTimeoutSeconds;

    @KafkaListener(
        topics = "auth.email-verification.requested",
        groupId = "notification-service"
    )
    @Transactional
    public void saveMessage(ConsumerRecord<String, VerifyEmailPayload> record){
        VerifyEmailPayload payload = record.value();
        if (payload == null) {
            return;
        }
        if (eventRepo.existsByEventId(payload.getEventId())) {
            return;
        }
        // Если прошлый retry упал после создания delivery, повтор Kafka
        // должен досоздать processed_events, а не падать на unique event_id.
        notificationRepo.findByEventId(payload.getEventId())
                .orElseGet(() -> saveNewNotification(payload));
        saveNewEvent(record);
    }

    private void sendEmail(VerifyEmailPayload payload, NotificationEntity notification){
        try {
            log.info("Sending email notification eventId={} recipient={}", notification.getEventId(), payload.getEmail());
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(from);
            helper.setTo(payload.getEmail());
            helper.setSubject("Подтверждение email");

            // HTML письмо с кнопкой-ссылкой.
            String html = """
                <html>
                  <body>
                    <h2>Подтверждение email</h2>
                    <p>Нажмите на ссылку ниже, чтобы подтвердить email:</p>
                    <p><a href="%s">Подтвердить email</a></p>
                  </body>
                </html>
                """.formatted(payload.getLink());

            helper.setText(html, true);

            // Реальная отправка письма через SMTP.
            mailSender.send(message);
            saveSentEvent(notification);
            log.info("Email notification sent eventId={} recipient={}", notification.getEventId(), payload.getEmail());
        } catch (Exception e) {
            log.warn("Email notification failed eventId={} recipient={} error={}", notification.getEventId(), payload.getEmail(), e.toString());
            saveFailedNotification(notification, e.toString());
        }
    }

    private void saveSentEvent(NotificationEntity notification){
        notification.setLastError(null);
        notification.setNextRetryAt(null);
        notification.setSentAt(LocalDateTime.now());
        notification.setStatus("SENT");
        notification.setUpdatedAt(LocalDateTime.now());
        notificationRepo.save(notification);
    }

    private void saveFailedNotification(NotificationEntity notification, String e){
        notification.setLastError(e);
        notification.setRetryCount(notification.getRetryCount() + 1);
        notification.setUpdatedAt(LocalDateTime.now());
        if (notification.getRetryCount() >= 5) {
            notification.setStatus("DEAD");
            notification.setNextRetryAt(null);
        } else {
            notification.setStatus("FAILED");
            notification.setNextRetryAt(LocalDateTime.now().plusSeconds(15));
        }
        notificationRepo.save(notification);
    }

    private NotificationEntity saveNewNotification(VerifyEmailPayload payload){
        NotificationEntity notification = new NotificationEntity();
        notification.setRetryCount(0);
        try {
            notification.setPayload(objectMapper.valueToTree(payload));
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось сериализовать payload уведомления", e);
        }
        notification.setChannel(payload.getChannel());
        notification.setCreatedAt(LocalDateTime.now());
        notification.setUpdatedAt(LocalDateTime.now());
        notification.setEventId(payload.getEventId());
        notification.setProviderMessageId(null);
        notification.setRecipient(payload.getEmail());
        notification.setTemplateCode("EMAIL_VERIFY_LINK");
        notification.setSubject("Подтверждение email");
        notification.setStatus("NEW");
        notificationRepo.save(notification);
        return notification;
    }
    private EventEntity saveNewEvent(ConsumerRecord<String, VerifyEmailPayload> record){
        VerifyEmailPayload payload = record.value();
        EventEntity event = new EventEntity();
        event.setConsumerGroup("notification-service");
        event.setEventId(payload.getEventId());
        event.setProcessedAt(LocalDateTime.now());
        event.setTopic(record.topic());
        event.setMessageOffset(record.offset());
        event.setPartitionNo(record.partition());
        event.setStatus("ACCEPTED");
        eventRepo.save(event);
        return event;
    }

    @Scheduled(fixedDelayString = "${app.notification-worker.fixed-delay-ms:15000}")
    public void sendMessage(){
        recoverStaleProcessing();
        List<NotificationEntity> failedNotifications = notificationRepo.findTop100ByStatusAndNextRetryAtBeforeOrderByNextRetryAtAsc("FAILED", LocalDateTime.now());
        List<NotificationEntity> newNotifications = notificationRepo.findTop100ByStatusOrderByCreatedAtAsc("NEW");
        sendAllMessage(failedNotifications);
        sendAllMessage(newNotifications);
    }

    private void recoverStaleProcessing() {
        LocalDateTime staleBefore = LocalDateTime.now().minusSeconds(processingTimeoutSeconds);
        List<NotificationEntity> staleNotifications = notificationRepo.findTop100ByStatusAndUpdatedAtBeforeOrderByUpdatedAtAsc("PROCESSING", staleBefore);
        for (NotificationEntity notification : staleNotifications) {
            // PROCESSING - промежуточный статус. Если сервис упал или SMTP завис,
            // возвращаем письмо в retry, чтобы оно не потерялось навсегда.
            notification.setStatus("FAILED");
            notification.setNextRetryAt(LocalDateTime.now());
            notification.setLastError("Recovered stale PROCESSING notification");
            notification.setUpdatedAt(LocalDateTime.now());
            notificationRepo.save(notification);
            log.warn("Recovered stale PROCESSING notification eventId={}", notification.getEventId());
        }
    }

    private void sendAllMessage(List<NotificationEntity> notifications){
        for (NotificationEntity notification : notifications) {
            VerifyEmailPayload payload = getPayload(notification);
            if (payload == null) {
                continue;
            }
            markProcessing(notification);
            sendEmail(payload, notification);
        }
    }

    private VerifyEmailPayload getPayload(NotificationEntity notification){
        try {
            return objectMapper.treeToValue(notification.getPayload(), VerifyEmailPayload.class);
        } catch (Exception e) {
            saveFailedNotification(notification, e.toString());
            return null;
        }
    }

    private void markProcessing(NotificationEntity notification) {
        notification.setStatus("PROCESSING");
        notification.setLastError(null);
        notification.setNextRetryAt(null);
        notification.setUpdatedAt(LocalDateTime.now());
        notificationRepo.save(notification);
    }
}
