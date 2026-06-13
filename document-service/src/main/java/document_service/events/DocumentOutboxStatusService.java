package document_service.events;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentOutboxStatusService {
    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public void markProcessing(OutboxEventEntity event) {
        event.setStatus("PROCESSING");
        event.setLastError(null);
        event.setNextRetryAt(null);
        outboxEventRepository.save(event);
    }

    @Transactional
    public void markPublished(UUID eventId) {
        outboxEventRepository.findByIdAndStatus(eventId, "PROCESSING").ifPresent(event -> {
            event.setStatus("PUBLISHED");
            event.setLastError(null);
            event.setNextRetryAt(null);
            event.setPublishedAt(LocalDateTime.now());
            outboxEventRepository.save(event);
        });
    }

    @Transactional
    public void markFailed(UUID eventId, String errorMessage) {
        outboxEventRepository.findByIdAndStatus(eventId, "PROCESSING").ifPresent(event -> {
            event.setLastError(errorMessage);
            event.setRetryCount(event.getRetryCount() + 1);
            if (event.getRetryCount() >= 5) {
                event.setStatus("DEAD");
                event.setNextRetryAt(null);
            } else {
                event.setStatus("FAILED");
                event.setNextRetryAt(LocalDateTime.now().plusSeconds(5));
            }
            outboxEventRepository.save(event);
        });
    }

    @Transactional
    public void markFailed(OutboxEventEntity event, String errorMessage) {
        event.setLastError(errorMessage);
        event.setRetryCount(event.getRetryCount() + 1);
        if (event.getRetryCount() >= 5) {
            event.setStatus("DEAD");
            event.setNextRetryAt(null);
        } else {
            event.setStatus("FAILED");
            event.setNextRetryAt(LocalDateTime.now().plusSeconds(5));
        }
        outboxEventRepository.save(event);
    }
}
