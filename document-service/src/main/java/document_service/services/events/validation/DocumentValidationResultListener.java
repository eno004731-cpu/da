package document_service.services.events.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import document_service.dto.payload.DocumentValidationResultPayload;
import document_service.persistence.document.DocumentEntity;
import document_service.persistence.document.DocumentRepository;
import document_service.persistence.document.DocumentValidationStatus;
import document_service.persistence.events.incoming.ProcessedEventEntity;
import document_service.persistence.events.incoming.ProcessedEventRepository;
import document_service.services.events.DocumentStatusService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Применяет результат проверки order-service к документу.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentValidationResultListener {
    private static final String EVENT_TYPE = "DOCUMENT_VALIDATION_RESULT";

    private final DocumentRepository documentRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final DocumentStatusService documentStatusService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroup;

    @KafkaListener(
            topics = "${app.kafka.topics.document-validation-result}",
            containerFactory = "documentValidationResultKafkaListenerContainerFactory"
    )
    @Transactional
    public void listen(ConsumerRecord<String, DocumentValidationResultPayload> record) {
        if (record == null || record.value() == null) {
            log.warn("Получено пустое событие результата проверки документа");
            return;
        }

        DocumentValidationResultPayload payload = record.value();
        Set<ConstraintViolation<DocumentValidationResultPayload>> violations =
                validator.validate(payload);
        if (!violations.isEmpty()) {
            String errors = violations.stream()
                    .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                    .collect(Collectors.joining(", "));
            log.warn("Некорректный результат проверки документа: {}", errors);
            return;
        }
        if (processedEventRepository.existsByEventId(payload.getEventId())) {
            log.info("Повторный результат проверки пропущен eventId={}", payload.getEventId());
            return;
        }

        ProcessedEventEntity processedEvent = receivedEvent(record, payload);
        DocumentEntity document = documentRepository
                .findByIdAndOrderId(payload.getDocumentId(), payload.getOrderId())
                .orElse(null);

        if (document == null) {
            documentStatusService.markDead(
                    processedEvent,
                    "Документ результата проверки не найден"
            );
            return;
        }

        if (Boolean.TRUE.equals(document.getIsDeleted()) || document.isDocumentDeleted()) {
            // Запоздавший ответ проверки не должен возвращать удаляемый документ в рабочее состояние.
            documentStatusService.markProcessed(processedEvent);
            log.info(
                    "Результат проверки проигнорирован для удалённого документа eventId={} documentId={}",
                    payload.getEventId(),
                    payload.getDocumentId()
            );
            return;
        }

        DocumentValidationStatus nextStatus = Boolean.TRUE.equals(payload.getValidationPassed())
                ? DocumentValidationStatus.DOCUMENT_VALIDATED
                : DocumentValidationStatus.DOCUMENT_REJECTED;

        document.setValidationStatus(nextStatus);
        document.setValidatedAt(payload.getValidatedAt());
        if (nextStatus == DocumentValidationStatus.DOCUMENT_REJECTED) {
            // Существующий recovery-процесс физически удалит файл с диска.
            document.setDocumentDeleted(true);
        }
        documentRepository.save(document);
        documentStatusService.markProcessed(processedEvent);

        log.info(
                "Статус проверки документа обновлён eventId={} documentId={} validationStatus={}",
                payload.getEventId(),
                payload.getDocumentId(),
                nextStatus
        );
    }

    private ProcessedEventEntity receivedEvent(
            ConsumerRecord<String, DocumentValidationResultPayload> record,
            DocumentValidationResultPayload payload
    ) {
        ProcessedEventEntity event = new ProcessedEventEntity();
        event.setEventId(payload.getEventId());
        event.setTopic(record.topic());
        event.setPartitionNo(record.partition());
        event.setMessageOffset(record.offset());
        event.setConsumerGroup(consumerGroup);
        event.setProcessedAt(LocalDateTime.now());
        event.setStatus("RECEIVED");
        event.setEventType(EVENT_TYPE);
        event.setRetryCount(0);
        event.setPayload(objectMapper.valueToTree(payload));
        return processedEventRepository.save(event);
    }
}
