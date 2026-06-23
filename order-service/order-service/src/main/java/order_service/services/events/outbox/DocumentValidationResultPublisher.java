package order_service.services.events.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import order_service.dto.payload.DocumentValidationResultPayload;
import order_service.persistence.events.outbox.OutboxEventEntity;
import order_service.persistence.events.outbox.OutboxEventRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Публикует результаты проверки из outbox в Kafka.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentValidationResultPublisher {
    private final OutboxEventRepo outboxEventRepo;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, DocumentValidationResultPayload> kafkaTemplate;
    private final EventStatusService eventStatusService;
    private final AtomicBoolean processing = new AtomicBoolean(false);

    @Value("${app.kafka.topics.document-validation-result}")
    private String topic;

    @Scheduled(fixedDelayString = "${app.outbox.document-validation-fixed-delay-ms}")
    @Async
    public void publishAvailableEvents() {
        if (!processing.compareAndSet(false, true)) {
            log.debug("Document validation outbox polling skipped: previous cycle is still running");
            return;
        }

        try {
            List<OutboxEventEntity> newEvents =
                    outboxEventRepo.findTop100ByStatusAndEventTypeOrderByCreatedAtAsc(
                            "NEW",
                            DocumentValidationOutboxService.EVENT_TYPE
                    );
            List<OutboxEventEntity> failedEvents =
                    outboxEventRepo.findTop100ByStatusAndEventTypeAndNextRetryAtBeforeOrderByNextRetryAtAsc(
                            "FAILED",
                            DocumentValidationOutboxService.EVENT_TYPE,
                            LocalDateTime.now()
                    );

            List<CompletableFuture<?>> sends = new ArrayList<>();
            sends.addAll(publish(newEvents));
            sends.addAll(publish(failedEvents));

            if (sends.isEmpty()) {
                processing.set(false);
                return;
            }

            // Флаг снимается только после завершения всех Kafka futures текущей пачки.
            CompletableFuture.allOf(sends.toArray(CompletableFuture[]::new))
                    .whenComplete((result, error) -> processing.set(false));
        } catch (RuntimeException exception) {
            processing.set(false);
            throw exception;
        }
    }

    private List<CompletableFuture<?>> publish(List<OutboxEventEntity> events) {
        List<CompletableFuture<?>> sends = new ArrayList<>();

        for (OutboxEventEntity event : events) {
            DocumentValidationResultPayload payload = readPayload(event);
            if (payload == null) {
                continue;
            }

            try {
                CompletableFuture<?> send = kafkaTemplate
                        .send(topic, payload.getDocumentId().toString(), payload)
                        .whenComplete((result, error) -> {
                            if (error == null) {
                                eventStatusService.savePublishedEvent(event);
                            } else {
                                eventStatusService.saveFailedEvent(event, error.toString());
                            }
                        });
                sends.add(send);
            } catch (RuntimeException exception) {
                eventStatusService.saveFailedEvent(event, exception.toString());
            }
        }

        return sends;
    }

    private DocumentValidationResultPayload readPayload(OutboxEventEntity event) {
        try {
            DocumentValidationResultPayload payload = objectMapper.treeToValue(
                    event.getPayload(),
                    DocumentValidationResultPayload.class
            );

            if (payload.getEventId() == null
                    || payload.getDocumentId() == null
                    || payload.getOrderId() == null
                    || payload.getValidationPassed() == null
                    || payload.getValidatedAt() == null) {
                eventStatusService.saveDeadEvent(event, "Неполный payload результата проверки документа");
                return null;
            }

            return payload;
        } catch (Exception exception) {
            eventStatusService.saveDeadEvent(event, exception.toString());
            return null;
        }
    }
}
