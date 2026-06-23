package document_service.services.events.validation;

import document_service.persistence.document.DocumentEntity;
import document_service.persistence.document.DocumentRepository;
import document_service.persistence.document.DocumentValidationStatus;
import document_service.persistence.events.outbox.OutboxEventRepository;
import document_service.services.events.DocumentOutboxEventFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Повторно запрашивает проверку документов, для которых ответ мог потеряться.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentValidationRetryService {
    private static final List<String> ACTIVE_OUTBOX_STATUSES =
            List.of("NEW", "PROCESSING", "FAILED");

    private final DocumentRepository documentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final DocumentOutboxEventFactory outboxEventFactory;

    @Value("${app.validation.retry-after-seconds}")
    private long retryAfterSeconds;

    @Scheduled(fixedDelayString = "${app.validation.retry-scan-delay-ms}")
    @Transactional
    public void createRetryEvents() {
        LocalDateTime requestedBefore = LocalDateTime.now().minusSeconds(retryAfterSeconds);
        List<DocumentEntity> documents =
                documentRepository
                        .findTop100ByValidationStatusAndIsDeletedFalseAndIsDocumentDeletedFalseAndValidationRequestedAtLessThanEqualOrderByValidationRequestedAtAsc(
                        DocumentValidationStatus.DOCUMENT_VALIDATION_REQUESTED,
                        requestedBefore
                );

        for (DocumentEntity document : documents) {
            boolean activeRequestExists =
                    outboxEventRepository.existsByAggregateIdAndEventTypeAndStatusIn(
                            document.getId().toString(),
                            DocumentOutboxEventFactory.DOCUMENT_STORED_EVENT_TYPE,
                            ACTIVE_OUTBOX_STATUSES
                    );
            if (activeRequestExists) {
                log.debug(
                        "Повторная проверка уже ожидает отправки documentId={}",
                        document.getId()
                );
                continue;
            }

            // Новый eventId обязателен: order-service идемпотентно пропускает старые eventId.
            outboxEventRepository.save(outboxEventFactory.documentStored(document));
            document.setValidationRequestedAt(LocalDateTime.now());
            documentRepository.save(document);
            log.info(
                    "Создан повторный запрос проверки documentId={} orderId={}",
                    document.getId(),
                    document.getOrderId()
            );
        }
    }
}
