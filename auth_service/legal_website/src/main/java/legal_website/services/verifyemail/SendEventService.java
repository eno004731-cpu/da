package legal_website.services.verifyemail;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import legal_website.dto.verifyemail.VerifyEmailPayload;
import legal_website.persistence.auth.UserEntity;
import legal_website.persistence.auth.UserRepo;
import legal_website.persistence.outbox_events.OutboxEventEntity;
import legal_website.persistence.outbox_events.OutboxEventsRepo;
import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
public class SendEventService {
    private final KafkaTemplate<String,VerifyEmailPayload> kafkaTemplate;
    private final OutboxEventsRepo outboxEventsRepo;
    private final ObjectMapper objectMapper;
    private final UserRepo userRepo;
    @Scheduled(fixedDelayString = "${app.outbox-relay.fixed-delay-ms:15000}")
    public void sendAllEvents(){
        processEvents(outboxEventsRepo.findTop100ByStatusOrderByCreatedAtAsc("NEW"));
        processEvents(outboxEventsRepo.findTop100ByStatusAndNextRetryAtBeforeOrderByNextRetryAtAsc("FAILED", LocalDateTime.now()));
    }

    private void processEvents(List<OutboxEventEntity> events) {
        for (OutboxEventEntity event : events) {
            processEvent(event);
        }
    }

    private void processEvent(OutboxEventEntity event) {
        UUID id = event.getId();
        VerifyEmailPayload payload;

        try {
            payload = objectMapper.treeToValue(event.getPayload(), VerifyEmailPayload.class);
        } catch (Exception e) {
            markCurrentEventFailed(event, e.toString());
            return;
        }

        Optional<UserEntity> userOptional = userRepo.findById(payload.getUserId());
        if (userOptional.isEmpty()) {
            markCurrentEventDead(event, "user not found");
            return;
        }

        UserEntity user = userOptional.get();
        if (!user.isActive()) {
            markCurrentEventDead(event, "inactive user");
            return;
        }

        if (user.isEmailVerified()) {
            markCurrentEventDead(event, "email already verified");
            return;
        }

        event.setStatus("PROCESSING");
        event.setLastError(null);
        event.setNextRetryAt(null);
        outboxEventsRepo.save(event);

        try {
            kafkaTemplate.send("auth.email-verification.requested", payload.getUserId().toString(), payload)
                .whenComplete((result, error) -> {
                    if (error == null) {
                        saveEventPUBLISHED(id);
                    } else {
                        saveEventFAILED(id, error.toString());
                    }
                });
        } catch (Exception e) {
            saveEventFAILED(id, e.toString());
        }
    }

    private void saveEventPUBLISHED(UUID id){
        outboxEventsRepo.findByIdAndStatus(id, "PROCESSING").ifPresent(event -> {
            event.setLastError(null);
            event.setNextRetryAt(null);
            event.setPublishedAt(LocalDateTime.now());
            event.setStatus("PUBLISHED");
            outboxEventsRepo.save(event);
        });
    }

    private void saveEventFAILED(UUID id, String errorMessage){
        outboxEventsRepo.findByIdAndStatus(id, "PROCESSING").ifPresent(event -> {
            applyFailedState(event, errorMessage);
            outboxEventsRepo.save(event);
        });
    }

    private void markCurrentEventFailed(OutboxEventEntity event, String errorMessage) {
        applyFailedState(event, errorMessage);
        outboxEventsRepo.save(event);
    }

    private void markCurrentEventDead(OutboxEventEntity event, String errorMessage) {
        event.setLastError(errorMessage);
        event.setRetryCount(5);
        event.setStatus("DEAD");
        event.setNextRetryAt(null);
        outboxEventsRepo.save(event);
    }

    private void applyFailedState(OutboxEventEntity event, String errorMessage) {
        event.setLastError(errorMessage);
        event.setRetryCount(event.getRetryCount() + 1);
        if (event.getRetryCount() >= 5) {
            event.setStatus("DEAD");
            event.setNextRetryAt(null);
        } else {
            event.setStatus("FAILED");
            event.setNextRetryAt(LocalDateTime.now().plusSeconds(5));
        }
    }
}
