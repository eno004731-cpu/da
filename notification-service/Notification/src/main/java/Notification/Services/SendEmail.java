package Notification.Services;

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

import Notification.Dto.VerityEmailPayload;
import Notification.EntityAndRepo.Events.EventEntity;
import Notification.EntityAndRepo.Events.EventRepo;
import Notification.EntityAndRepo.Nofilication.NofilicationEntity;
import Notification.EntityAndRepo.Nofilication.NofilicationRepo;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SendEmail {
    private final EventRepo eventRepo;
    private final NofilicationRepo nofilicationRepo;
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
    public void saveMesssage(ConsumerRecord<String, VerityEmailPayload> record){
        VerityEmailPayload payload = record.value();
        if (payload == null) {
            return;
        }
        if (eventRepo.existsByEventId(payload.getEventId())) {
            return;
        }
        // Если прошлый retry упал после создания delivery, повтор Kafka
        // должен досоздать processed_events, а не падать на unique event_id.
        nofilicationRepo.findByEventId(payload.getEventId())
                .orElseGet(() -> saveNewNofilication(payload));
        saveNewEvent(record);
    }

    private void sendEmail(VerityEmailPayload payload, NofilicationEntity nofilication){
        try {
            log.info("Sending email notification eventId={} recipient={}", nofilication.getEventId(), payload.getEmail());
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
            saveSentEvent(nofilication);
            log.info("Email notification sent eventId={} recipient={}", nofilication.getEventId(), payload.getEmail());
        } catch (Exception e) {
            log.warn("Email notification failed eventId={} recipient={} error={}", nofilication.getEventId(), payload.getEmail(), e.toString());
            saveFailedNofilication(nofilication, e.toString());
        }
    }

    private void saveSentEvent(NofilicationEntity nofilication){
        nofilication.setLastError(null);
        nofilication.setNextRetryAt(null);
        nofilication.setSentAt(LocalDateTime.now());
        nofilication.setStatus("SENT");
        nofilication.setUpdatedAt(LocalDateTime.now());
        nofilicationRepo.save(nofilication);
    }

    private void saveFailedNofilication(NofilicationEntity nofilication, String e){
        nofilication.setLastError(e);
        nofilication.setRetryCount(nofilication.getRetryCount() + 1);
        nofilication.setUpdatedAt(LocalDateTime.now());
        if (nofilication.getRetryCount() >= 5) {
            nofilication.setStatus("DEAD");
            nofilication.setNextRetryAt(null);
        } else {
            nofilication.setStatus("FAILED");
            nofilication.setNextRetryAt(LocalDateTime.now().plusSeconds(15));
        }
        nofilicationRepo.save(nofilication);
    }

    private NofilicationEntity saveNewNofilication(VerityEmailPayload payload){
        NofilicationEntity nofilication = new NofilicationEntity();
        nofilication.setRetryCount(0);
        try {
            nofilication.setPayload(objectMapper.valueToTree(payload));
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось сериализовать payload уведомления", e);
        }
        nofilication.setChannel(payload.getChannel());
        nofilication.setCreatedAt(LocalDateTime.now());
        nofilication.setUpdatedAt(LocalDateTime.now());
        nofilication.setEventId(payload.getEventId());
        nofilication.setProviderMessageId(null);
        nofilication.setRecipient(payload.getEmail());
        nofilication.setTemplateCode("EMAIL_VERIFY_LINK");
        nofilication.setSubject("Подтверждение email");
        nofilication.setStatus("NEW");
        nofilicationRepo.save(nofilication);
        return nofilication;
    }
    private EventEntity saveNewEvent(ConsumerRecord<String, VerityEmailPayload> record){
        VerityEmailPayload payload = record.value();
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
        List<NofilicationEntity> failedNofilications = nofilicationRepo.findTop100ByStatusAndNextRetryAtBeforeOrderByNextRetryAtAsc("FAILED", LocalDateTime.now());
        List<NofilicationEntity> newNofilications = nofilicationRepo.findTop100ByStatusOrderByCreatedAtAsc("NEW");
        sendAllMessage(failedNofilications);
        sendAllMessage(newNofilications);
    }

    private void recoverStaleProcessing() {
        LocalDateTime staleBefore = LocalDateTime.now().minusSeconds(processingTimeoutSeconds);
        List<NofilicationEntity> staleNofilications = nofilicationRepo.findTop100ByStatusAndUpdatedAtBeforeOrderByUpdatedAtAsc("PROCESSING", staleBefore);
        for (NofilicationEntity nofilication : staleNofilications) {
            // PROCESSING - промежуточный статус. Если сервис упал или SMTP завис,
            // возвращаем письмо в retry, чтобы оно не потерялось навсегда.
            nofilication.setStatus("FAILED");
            nofilication.setNextRetryAt(LocalDateTime.now());
            nofilication.setLastError("Recovered stale PROCESSING notification");
            nofilication.setUpdatedAt(LocalDateTime.now());
            nofilicationRepo.save(nofilication);
            log.warn("Recovered stale PROCESSING notification eventId={}", nofilication.getEventId());
        }
    }

    private void sendAllMessage(List<NofilicationEntity> nofilications){
        for (NofilicationEntity nofilication : nofilications) {
            VerityEmailPayload payload = getPayload(nofilication);
            if (payload == null) {
                continue;
            }
            markProcessing(nofilication);
            sendEmail(payload, nofilication);
        }
    }

    private VerityEmailPayload getPayload(NofilicationEntity nofilication){
        try {
            return objectMapper.treeToValue(nofilication.getPayload(), VerityEmailPayload.class);
        } catch (Exception e) {
            saveFailedNofilication(nofilication, e.toString());
            return null;
        }
    }

    private void markProcessing(NofilicationEntity nofilication) {
        nofilication.setStatus("PROCESSING");
        nofilication.setLastError(null);
        nofilication.setNextRetryAt(null);
        nofilication.setUpdatedAt(LocalDateTime.now());
        nofilicationRepo.save(nofilication);
    }
}
