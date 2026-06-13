package document_service.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class DocumentOutboxPublisher {
    private final OutboxEventRepository outboxEventRepository;
    private final DocumentOutboxStatusService statusService;
    private final KafkaTemplate<String, DocumentStoredPayload> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean processing = new AtomicBoolean(false);

    @Value("${app.kafka.topics.document-stored}")
    private String documentStoredTopic;

    @Scheduled(fixedDelayString = "${app.outbox-relay.fixed-delay-ms:5000}")
    public void publishAvailableEvents() {
        if (!processing.compareAndSet(false, true)) {
            return;
        }

        try {
            publishEvents(outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc("NEW"));
            publishEvents(outboxEventRepository.findTop100ByStatusAndNextRetryAtBeforeOrderByNextRetryAtAsc("FAILED", LocalDateTime.now()));
        } finally {
            processing.set(false);
        }
    }

    private void publishEvents(List<OutboxEventEntity> events) {
        for (OutboxEventEntity event : events) {
            if (!"DOCUMENT_STORED".equals(event.getEventType())) {
                statusService.markFailed(event, "Unsupported document outbox event type: " + event.getEventType());
                continue;
            }

            DocumentStoredPayload payload = readPayload(event);
            if (payload == null) {
                continue;
            }

            UUID eventId = event.getId();
            statusService.markProcessing(event);
            try {
                kafkaTemplate.send(documentStoredTopic, payload.getOrderId().toString(), payload)
                        .whenComplete((result, error) -> {
                            if (error == null) {
                                statusService.markPublished(eventId);
                            } else {
                                statusService.markFailed(eventId, error.toString());
                            }
                        });
            } catch (Exception e) {
                statusService.markFailed(eventId, e.toString());
            }
        }
    }

    private DocumentStoredPayload readPayload(OutboxEventEntity event) {
        try {
            return objectMapper.treeToValue(event.getPayload(), DocumentStoredPayload.class);
        } catch (Exception e) {
            statusService.markFailed(event, e.toString());
            return null;
        }
    }
}
