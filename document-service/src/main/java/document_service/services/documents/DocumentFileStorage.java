package document_service.services.documents;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Отвечает только за физическое хранение файлов и подготовку файловых метаданных.
 */
@Component
public class DocumentFileStorage {
    private final Path documentsDir;

    public DocumentFileStorage(@Value("${app.storage.documents-dir}") String documentsDir) {
        // Один раз нормализуем корневую директорию, чтобы все операции использовали единый путь.
        this.documentsDir = Path.of(documentsDir).toAbsolutePath().normalize();
    }

    public StoredDocumentFile store(UUID orderId, MultipartFile document) {
        if (document == null || document.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Пустые документы загружать нельзя");
        }

        String originalFileName = resolveOriginalFileName(document);
        String storageKey = orderId + "/" + UUID.randomUUID() + "-" + originalFileName;
        Path storagePath = resolveStoragePath(storageKey);

        try {
            // Для каждого заказа директория создаётся лениво при первой загрузке.
            Files.createDirectories(storagePath.getParent());
            document.transferTo(storagePath);
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Не удалось сохранить документ",
                    exception
            );
        }

        return new StoredDocumentFile(
                originalFileName,
                storageKey,
                resolveMimeType(document),
                document.getSize()
        );
    }

    public void deleteIfExists(String storageKey) {
        Path storagePath = resolveStoragePath(storageKey);

        try {
            // Повторное удаление безопасно: отсутствующий файл не считается ошибкой.
            Files.deleteIfExists(storagePath);
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Не удалось удалить документ",
                    exception
            );
        }
    }

    private Path resolveStoragePath(String storageKey) {
        Path storagePath = documentsDir.resolve(storageKey).normalize();

        // Не разрешаем сформированному пути выйти за пределы директории документов.
        if (!storagePath.startsWith(documentsDir)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Некорректный путь документа");
        }

        return storagePath;
    }

    private String resolveOriginalFileName(MultipartFile document) {
        String cleanPath = StringUtils.cleanPath(
                document.getOriginalFilename() == null ? "" : document.getOriginalFilename()
        );
        String fileName = StringUtils.getFilename(cleanPath);

        // Для отсутствующего имени используем безопасное техническое значение.
        return fileName == null || fileName.isBlank() ? "document.bin" : fileName;
    }

    private String resolveMimeType(MultipartFile document) {
        String contentType = document.getContentType();

        // MIME уже проверяется через Tika раньше, fallback нужен для целостности метаданных.
        return contentType == null || contentType.isBlank()
                ? "application/octet-stream"
                : contentType;
    }

    /**
     * Результат физического сохранения, необходимый для создания записи в БД.
     */
    public record StoredDocumentFile(
            String originalFileName,
            String storageKey,
            String mimeType,
            long size
    ) {
    }
}
