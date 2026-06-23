package document_service.services.events;

import document_service.persistence.events.outbox.OutboxEventEntity;
import document_service.persistence.events.outbox.OutboxEventRepository;
import document_service.persistence.events.incoming.ProcessedEventEntity;
import document_service.persistence.events.incoming.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentStatusService {
    private final OutboxEventRepository outboxEventRepository;
    private final ProcessedEventRepository processedEventRepository;

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

    @Transactional
    public void markFailed(ProcessedEventEntity event, String errorMessage) {
        // FAILED оставляет событие доступным для контролируемого повторного запуска.
        event.setStatus("FAILED");
        event.setErrorMessage(errorMessage);
        processedEventRepository.save(event);
    }

    @Transactional
    public void markDead(ProcessedEventEntity event, String errorMessage) {
        // DEAD означает, что автоматический повтор уже не сможет исправить событие.
        event.setStatus("DEAD");
        event.setErrorMessage(errorMessage);
        event.setNextRetryAt(null);
        processedEventRepository.save(event);
    }

    @Transactional
    public void markProcessed(ProcessedEventEntity event) {
        // После успешной обработки очищаем диагностические поля предыдущих попыток.
        event.setStatus("PROCESSED");
        event.setErrorMessage(null);
        event.setNextRetryAt(null);
        processedEventRepository.save(event);
    }
}
