
package document_service.services.documents.delete;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import document_service.dto.payload.DocumentToDeletePayload;
import document_service.persistence.document.DocumentEntity;
import document_service.persistence.document.DocumentRepository;
import document_service.persistence.events.incoming.ProcessedEventEntity;
import document_service.persistence.events.incoming.ProcessedEventRepository;
import document_service.services.documents.store.DocumentFileStorage;
import document_service.services.events.DocumentStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentsDeleteService {
    private static final String RECEIVED_STATUS = "RECEIVED";
    private static final String DELETE_DOCUMENT_EVENT = "DELETE_DOCUMENT";

    private final ProcessedEventRepository processedEventRepository;
    private final DocumentRepository documentRepository;
    private final DocumentFileStorage documentFileStorage;
    private final DocumentStatusService documentStatusService;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5000)
    public void markDocumentDelete() {
        // Первый список содержит новые команды, второй позволяет продолжить незавершённое удаление.
        List<ProcessedEventEntity> deleteEvents =
                processedEventRepository.findAllByStatusAndEventType(RECEIVED_STATUS, DELETE_DOCUMENT_EVENT);
        List<DocumentEntity> documentsWaitingForDeletion =
                documentRepository.findAllByIsDocumentDeletedTrueAndIsDeletedFalse();

        log.debug(
                "Запущена обработка удаления документов: receivedEvents={}, recoveryDocuments={}",
                deleteEvents.size(),
                documentsWaitingForDeletion.size()
        );

        if (deleteEvents.isEmpty() && documentsWaitingForDeletion.isEmpty()) {
            log.debug("Нет событий и документов, ожидающих удаления");
            return;
        }

        Set<Long> processedDocumentIds = new HashSet<>();

        for (ProcessedEventEntity deleteEvent : deleteEvents) {
            log.debug("Начата обработка события удаления: eventId={}", deleteEvent.getEventId());

            Optional<DocumentToDeletePayload> payload = readPayload(deleteEvent);
            if (payload.isEmpty()) {
                continue;
            }

            Optional<Long> documentId = readDocumentId(deleteEvent, payload.get());
            if (documentId.isEmpty()) {
                continue;
            }

            Optional<DocumentEntity> document = documentRepository.findByIdAndOrderId(
                    documentId.get(),
                    payload.get().getOrderId()
            );

            if (document.isEmpty()) {
                log.warn(
                        "Документ из события удаления не найден: eventId={}, documentId={}, orderId={}",
                        deleteEvent.getEventId(),
                        documentId.get(),
                        payload.get().getOrderId()
                );
                documentStatusService.markFailed(deleteEvent, "Документ из события удаления не найден");
                continue;
            }

            try {
                deleteDocument(document.get());
                processedDocumentIds.add(document.get().getId());
                documentStatusService.markProcessed(deleteEvent);
                log.info(
                        "Документ успешно удалён: eventId={}, documentId={}, orderId={}",
                        deleteEvent.getEventId(),
                        document.get().getId(),
                        document.get().getOrderId()
                );
            } catch (RuntimeException exception) {
                // Событие остаётся RECEIVED, поэтому следующий запуск повторит удаление.
                deleteEvent.setErrorMessage(exception.getMessage());
                processedEventRepository.save(deleteEvent);
                log.error("Не удалось удалить документ для eventId={}", deleteEvent.getEventId(), exception);
            }
        }

        for (DocumentEntity document : documentsWaitingForDeletion) {
            // Документ мог быть обработан выше по событию из первого списка.
            if (processedDocumentIds.contains(document.getId())) {
                log.debug(
                        "Recovery-удаление пропущено, документ уже обработан по событию: documentId={}",
                        document.getId()
                );
                continue;
            }

            try {
                log.debug("Продолжено незавершённое удаление документа: documentId={}", document.getId());
                deleteDocument(document);
                log.info(
                        "Незавершённое удаление документа успешно завершено: documentId={}, orderId={}",
                        document.getId(),
                        document.getOrderId()
                );
            } catch (RuntimeException exception) {
                log.error("Не удалось завершить удаление документа id={}", document.getId(), exception);
            }
        }
    }

    private Optional<DocumentToDeletePayload> readPayload(ProcessedEventEntity deleteEvent) {
        if (deleteEvent.getPayload() == null || deleteEvent.getPayload().isNull()) {
            log.warn("Payload события удаления отсутствует: eventId={}", deleteEvent.getEventId());
            documentStatusService.markDead(deleteEvent, "Payload события удаления отсутствует");
            return Optional.empty();
        }

        try {
            // JSONB преобразуется обратно в тот же DTO, который listener получил из Kafka.
            return Optional.of(
                    objectMapper.treeToValue(deleteEvent.getPayload(), DocumentToDeletePayload.class)
            );
        } catch (JsonProcessingException exception) {
            log.warn(
                    "Не удалось прочитать payload события удаления: eventId={}",
                    deleteEvent.getEventId(),
                    exception
            );
            documentStatusService.markDead(
                    deleteEvent,
                    "Не удалось прочитать payload события удаления: " + exception.getMessage()
            );
            return Optional.empty();
        }
    }

    private Optional<Long> readDocumentId(
            ProcessedEventEntity deleteEvent,
            DocumentToDeletePayload payload
    ) {
        try {
            // documentId из payload соответствует BIGINT primary key таблицы order_documents.
            return Optional.of(Long.valueOf(payload.getDocumentId()));
        } catch (NumberFormatException exception) {
            log.warn(
                    "Некорректный documentId в payload: eventId={}, documentId={}",
                    deleteEvent.getEventId(),
                    payload.getDocumentId()
            );
            documentStatusService.markDead(
                    deleteEvent,
                    "Некорректный documentId в payload: " + payload.getDocumentId()
            );
            return Optional.empty();
        }
    }

    private void deleteDocument(DocumentEntity document) {
        if (!document.isDocumentDeleted()) {
            log.debug("Устанавливается признак начала удаления: documentId={}", document.getId());

            // Сначала сохраняем recovery-маркер, чтобы удаление можно было продолжить после сбоя.
            document.setDocumentDeleted(true);
            documentRepository.saveAndFlush(document);
        }

        log.debug("Удаляется файл документа с диска: documentId={}", document.getId());

        // Удаление отсутствующего файла безопасно, поэтому повторный запуск не повредит данным.
        documentFileStorage.deleteIfExists(document.getStorageKey());

        // Финальный статус фиксируем только после успешного удаления файла с диска.
        document.setIsDeleted(true);
        document.setDeletedAt(LocalDateTime.now());
        documentRepository.saveAndFlush(document);
    }

}
