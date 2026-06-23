package document_service.services.events.delete;

import document_service.dto.payload.DocumentToDeletePayload;
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

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentsToDeleteListener {
    private final ProcessedEventRepository processedEventRepo;
    private final Validator validator;
    private final ObjectMapper objectMapper;
    private final DocumentStatusService documentStatusService;
    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroup;

    @KafkaListener(
            topics = "${app.kafka.topics.document-delete-requested}",
            containerFactory = "documentDeleteKafkaListenerContainerFactory"
    )
    public void listenerKafka(ConsumerRecord<String, DocumentToDeletePayload> record) {
        if (record == null) {
            log.warn("Получена пустая Kafka-запись для удаления документа");
            return;
        }

        log.info(
                "Получен запрос на удаление документа: topic={}, partition={}, offset={}, key={}",
                record.topic(),
                record.partition(),
                record.offset(),
                record.key()
        );

        DocumentToDeletePayload payload = record.value();
        if (payload == null) {
            log.warn(
                    "Kafka-сообщение не содержит payload: topic={}, partition={}, offset={}",
                    record.topic(),
                    record.partition(),
                    record.offset()
            );
            return;
        }
        // Правила корректности payload описаны в самом DTO через Bean Validation.
        Set<ConstraintViolation<DocumentToDeletePayload>> violations = validator.validate(payload);
        if (!violations.isEmpty()) {
            String validationErrors = violations.stream()
                    .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                    .collect(Collectors.joining(", "));

            log.warn(
                    "Некорректный payload запроса на удаление документа: errors={}, "
                            + "topic={}, partition={}, offset={}",
                    validationErrors,
                    record.topic(),
                    record.partition(),
                    record.offset()
            );
            return;
        }
        if (processedEventRepo.existsByEventId(payload.getEventId())) {
            log.info(
                    "Повторное событие удаления документа пропущено: eventId={}, orderId={}, documentId={}",
                    payload.getEventId(),
                    payload.getOrderId(),
                    payload.getDocumentId()
            );
            return;
        }

        saveEvent(record);
        log.info(
                "Событие удаления документа обработано: eventId={}, orderId={}, documentId={}",
                payload.getEventId(),
                payload.getOrderId(),
                payload.getDocumentId()
        );
   }
        @Transactional
        private void saveEvent(ConsumerRecord<String,DocumentToDeletePayload> record){
                ProcessedEventEntity processedEvent = new ProcessedEventEntity();
                // eventId нужен для идемпотентности, а данные документа сохраняются ниже в JSONB payload.
                processedEvent.setEventId(record.value().getEventId());
                processedEvent.setTopic(record.topic());
                processedEvent.setPartitionNo(record.partition());
                processedEvent.setMessageOffset(record.offset());
                processedEvent.setConsumerGroup(consumerGroup);
                processedEvent.setProcessedAt(LocalDateTime.now());
                processedEvent.setStatus("RECEIVED");
                processedEvent.setRetryCount(0);
                processedEvent.setEventType("DELETE_DOCUMENT");
                try {
                        processedEvent.setPayload(objectMapper.valueToTree(record.value()));
                } catch (Exception e) {
                        // JSON null сохраняет обязательное поле payload, а DEAD запрещает бессмысленные повторы.
                        processedEvent.setPayload(objectMapper.nullNode());
                        documentStatusService.markDead(
                                processedEvent,
                                "Не удалось сериализовать payload события удаления: " + e.getMessage()
                        );
                        log.error(
                                "Не удалось сериализовать payload события удаления: eventId={}",
                                record.value().getEventId(),
                                e
                        );
                        return;
                }
                processedEventRepo.save(processedEvent);

    }
}
