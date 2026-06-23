package legal_website.services.delete;

import com.fasterxml.jackson.databind.ObjectMapper;
import legal_website.dto.DeletePayload;
import legal_website.persistence.auth.UserDeletionStatus;
import legal_website.persistence.auth.UserEntity;
import legal_website.persistence.auth.UserRepo;
import legal_website.persistence.outbox_events.OutboxEventEntity;
import legal_website.services.outbox.OutboxEventStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeleteProducer {
    private final UserRepo userRepo;
    private final KafkaTemplate<String, DeletePayload> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final DeleteOutboxClaimService deleteOutboxClaimService;
    private final OutboxEventStatusService outboxEventStatusService;
    private final ThreadPoolTaskExecutor deleteOutboxCallbackExecutor;
    private final AtomicBoolean schedulerRunning = new AtomicBoolean(false);

    @Value("${app.kafka.topics.delete-user-orders}")
    private String deleteUserOrdersTopic;

    @Value("${app.kafka.topics.delete-user-documents}")
    private String deleteUserDocumentsTopic;

    @Value("${app.outbox.delete.processing-timeout-ms:180000}")
    private long processingTimeoutMs;

    /**
     * fixedDelay отсчитывает пять секунд после завершения предыдущего прохода.
     * AtomicBoolean дополнительно защищает от ручного/параллельного запуска
     * scheduler-метода внутри одного экземпляра приложения.
     */
    @Scheduled(fixedDelayString = "${app.outbox.delete.fixed-delay-ms:5000}")
    public void publishAvailableEvents() {
        if (!schedulerRunning.compareAndSet(false, true)) {
            log.debug("Delete outbox scan skipped: previous scan is running");
            return;
        }

        try {
            LocalDateTime now = LocalDateTime.now();
            List<OutboxEventEntity> events =
                    deleteOutboxClaimService.claimAvailableEvents(
                            now,
                            now.minus(Duration.ofMillis(processingTimeoutMs))
                    );

            if (!events.isEmpty()) {
                log.info(
                        "Delete outbox scan claimed events count={}",
                        events.size()
                );
            }

            events.forEach(this::sendAndCreateEvent);
        } catch (RuntimeException exception) {
            log.error("Delete outbox scan failed", exception);
        } finally {
            schedulerRunning.set(false);
        }
    }

    private void sendAndCreateEvent(OutboxEventEntity event) {
        try {
            Long userId = Long.valueOf(event.getAggregateId());
            Optional<UserEntity> userOptional =
                    userRepo.findByIdWithDeletionProcess(userId);

            if (userOptional.isEmpty()) {
                outboxEventStatusService.markDead(event, "user not found");
                return;
            }

            UserEntity user = userOptional.get();
            String invalidState = validateDeletionState(user);
            if (invalidState != null) {
                outboxEventStatusService.markDead(event, invalidState);
                return;
            }

            DeletePayload deletePayload = readPayload(event);
            if (deletePayload == null) {
                return;
            }
            if (!userId.equals(deletePayload.getId())) {
                outboxEventStatusService.markDead(
                        event,
                        "payload userId does not match aggregateId"
                );
                return;
            }

            send(event, deletePayload, topicFor(event));
        } catch (NumberFormatException exception) {
            outboxEventStatusService.markDead(
                    event,
                    "aggregateId must contain numeric userId"
            );
        } catch (RuntimeException exception) {
            outboxEventStatusService.markFailed(
                    event.getId(),
                    event.getProcessingToken(),
                    exception.toString()
            );
        }
    }

    private String validateDeletionState(UserEntity user) {
        if (user.isActive()) {
            return "user is still active";
        }
        if (user.getDeletionStatus()
                != UserDeletionStatus.DELETION_IN_PROGRESS) {
            return "user deletion is not in progress";
        }
        if (user.getDeletionProcess() == null) {
            return "user deletion process not found";
        }
        return null;
    }

    private String topicFor(OutboxEventEntity event) {
        DeleteOutboxEventType eventType;
        try {
            eventType = DeleteOutboxEventType.valueOf(event.getEventType());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "unsupported deletion event type: " + event.getEventType(),
                    exception
            );
        }

        return switch (eventType) {
            case DELETE_ALL_ORDERS -> deleteUserOrdersTopic;
            case DELETE_ALL_DOCUMENTS -> deleteUserDocumentsTopic;
        };
    }

    private void send(
            OutboxEventEntity event,
            DeletePayload payload,
            String topic
    ) {
        // Сохраняем токен текущей попытки для защиты от старого callback.
        UUID processingToken = event.getProcessingToken();

        log.info(
                "Publishing delete event eventId={} eventType={} userId={} topic={}",
                event.getId(),
                event.getEventType(),
                payload.getId(),
                topic
        );

        try {
            kafkaTemplate.send(
                            topic,
                            event.getAggregateId(),
                            payload
                    )
                    .whenCompleteAsync((result, error) -> {
                        if (error != null) {
                            log.warn(
                                    "Delete event publish failed eventId={} eventType={} error={}",
                                    event.getId(),
                                    event.getEventType(),
                                    error.toString()
                            );
                            outboxEventStatusService.markFailed(
                                    event.getId(),
                                    processingToken,
                                    error.toString()
                            );
                        } else {
                            log.info(
                                    "Delete event published eventId={} eventType={} topic={} partition={} offset={}",
                                    event.getId(),
                                    event.getEventType(),
                                    result.getRecordMetadata().topic(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset()
                            );
                            outboxEventStatusService.markPublished(
                                    event.getId(),
                                    processingToken
                            );
                        }
                    }, deleteOutboxCallbackExecutor
                    );
        } catch (RuntimeException exception) {
            log.warn(
                    "Delete event send failed synchronously eventId={} eventType={}",
                    event.getId(),
                    event.getEventType(),
                    exception
            );
            outboxEventStatusService.markFailed(
                    event.getId(),
                    processingToken,
                    exception.toString()
            );
        }
    }

    private DeletePayload readPayload(OutboxEventEntity event) {
        try {
            return objectMapper.treeToValue(
                    event.getPayload(),
                    DeletePayload.class
            );
        } catch (Exception exception) {
            // Повторная отправка не исправит повреждённый JSON.
            outboxEventStatusService.markDead(
                    event,
                    "invalid delete payload: " + exception
            );
            return null;
        }
    }

}
