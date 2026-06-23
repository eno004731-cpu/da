package legal_website.services.verifyemail;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import legal_website.dto.verifyemail.VerifyEmailPayload;
import legal_website.persistence.auth.UserEntity;
import legal_website.persistence.auth.UserRepo;
import legal_website.persistence.outbox_events.OutboxEventEntity;
import legal_website.persistence.outbox_events.OutboxEventsRepo;
import legal_website.services.outbox.OutboxEventStatusService;
import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
public class SendEventService {
    private final KafkaTemplate<String,VerifyEmailPayload> kafkaTemplate;
    private final OutboxEventsRepo outboxEventsRepo;
    private final ObjectMapper objectMapper;
    private final UserRepo userRepo;
    private final OutboxEventStatusService outboxEventStatusService;

    @Scheduled(fixedDelayString = "${app.outbox-relay.fixed-delay-ms:15000}")
    public void sendAllEvents(){
        processEvents(
                outboxEventsRepo
                        .findTop100ByStatusAndEventTypeOrderByCreatedAtAsc(
                                "NEW",
                                "EMAIL_VERIFICATION_REQUESTED"
                        )
        );
        processEvents(
                outboxEventsRepo
                        .findTop100ByStatusAndEventTypeAndNextRetryAtBeforeOrderByNextRetryAtAsc(
                                "FAILED",
                                "EMAIL_VERIFICATION_REQUESTED",
                                LocalDateTime.now()
                        )
        );
    }

    private void processEvents(List<OutboxEventEntity> events) {
        for (OutboxEventEntity event : events) {
            processEvent(event);
        }
    }

    private void processEvent(OutboxEventEntity event) {
        VerifyEmailPayload payload;

        try {
            payload = objectMapper.treeToValue(event.getPayload(), VerifyEmailPayload.class);
        } catch (Exception e) {
            outboxEventStatusService.markFailed(event, e.toString());
            return;
        }

        Optional<UserEntity> userOptional = userRepo.findById(payload.getUserId());
        if (userOptional.isEmpty()) {
            outboxEventStatusService.markDead(event, "user not found");
            return;
        }

        UserEntity user = userOptional.get();
        if (!user.isActive()) {
            outboxEventStatusService.markDead(event, "inactive user");
            return;
        }

        if (user.isEmailVerified()) {
            outboxEventStatusService.markDead(event, "email already verified");
            return;
        }

        outboxEventStatusService.markProcessing(event);

        try {
            kafkaTemplate.send("auth.email-verification.requested", payload.getUserId().toString(), payload)
                .whenComplete((result, error) -> {
                    if (error == null) {
                        outboxEventStatusService.markPublished(event.getId());
                    } else {
                        outboxEventStatusService.markFailed(
                                event.getId(),
                                error.toString()
                        );
                    }
                });
        } catch (Exception e) {
            outboxEventStatusService.markFailed(event.getId(), e.toString());
        }
    }
}
