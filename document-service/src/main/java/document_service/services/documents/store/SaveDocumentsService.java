package document_service.services.documents.store;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.tika.Tika;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import document_service.dto.request.SaveRequest;
import document_service.dto.response.SaveResponse;
import document_service.dto.response.UploadedDocumentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SaveDocumentsService {

    private final OrderDocumentsService orderDocumentsService;

    // Tika определяет MIME-тип по содержимому файла, а не по данным от клиента.
    private static final Tika TIKA = new Tika();

    // Белый список MIME-типов, которые разрешено принимать от клиента.
    private static final Set<String> ALLOWED_FILE_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png"
    );

    public List<SaveResponse> saveDocument(SaveRequest saveRequest) {
        if (saveRequest == null
                || saveRequest.getDocuments() == null
                || saveRequest.getDocuments().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Нужно передать хотя бы один документ"
            );
        }

        // До сохранения оставляем только файлы, реальный MIME-тип которых разрешён.
        List<MultipartFile> allowedDocuments = saveRequest.getDocuments().stream()
                .filter(this::isAllowedFileType)
                .toList();

        if (allowedDocuments.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Нет документов разрешённого типа"
            );
        }

        // Существующий доменный сервис сохраняет файлы и метаданные в БД.
        return orderDocumentsService.uploadDocuments(
                        saveRequest.getOrderId(),
                        saveRequest.getUserId(),
                        allowedDocuments
                ).stream()
                .map(document -> buildSaveResponse(saveRequest.getOrderId(), document))
                .toList();
    }

    /**
     * Собирает DTO ответа из метаданных уже сохранённого документа.
     *
     * @param orderId идентификатор заказа документа
     * @param document метаданные документа после сохранения
     * @return DTO для отправки frontend
     */
    private SaveResponse buildSaveResponse(UUID orderId, UploadedDocumentResponse document) {
        SaveResponse response = new SaveResponse();

        // Идентификатор уже создан БД и может использоваться для проверки статуса.
        response.setDocumentId(Long.valueOf(document.getId()));
        response.setOrderId(orderId);
        response.setFileName(document.getFileName());
        response.setMimeType(document.getMimeType());
        response.setSize(document.getSize());
        response.setUploadedAt(document.getUploadedAt());
        response.setDownloadUrl(document.getDownloadUrl());
        response.setDeleted(document.isDeleted());
        response.setDeletedAt(document.getDeletedAt());
        response.setValidationStatus(document.getValidationStatus());

        return response;
    }

    /**
     * Проверяет реальный MIME-тип файла по содержимому до вызова transferTo().
     *
     * @param document файл, полученный в multipart-запросе
     * @return true, если определённый Tika MIME-тип входит в белый список
     */
    private boolean isAllowedFileType(MultipartFile document) {
        if (document == null || document.isEmpty()) {
            log.warn("Document is empty");
            return false;
        }

        try (InputStream inputStream = document.getInputStream()) {
            // detect() читает сигнатуру файла и возвращает фактический MIME-тип.
            String detectedType = TIKA.detect(inputStream);

            if (!ALLOWED_FILE_TYPES.contains(detectedType)) {
                log.warn(
                        "Document type is not allowed: fileName={}, declaredType={}, detectedType={}",
                        document.getOriginalFilename(),
                        document.getContentType(),
                        detectedType
                );
                return false;
            }

            return true;
        } catch (IOException exception) {
            // Если содержимое невозможно прочитать, файл нельзя считать безопасным.
            log.warn(
                    "Could not detect document type: fileName={}",
                    document.getOriginalFilename(),
                    exception
            );
            return false;
        }
    }
}
