package my_jira.orders.create;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import my_jira.common.exception.FileValidationException;
import my_jira.common.exception.ServiceNotFoundException;
import my_jira.common.exception.StorageOperationException;
import my_jira.common.exception.UserNotFoundException;
import my_jira.documents.DocumentsRepo;
import my_jira.documents.OrdersDocuments;
import my_jira.orders.OrdersEntity;
import my_jira.orders.OrdersRepo;
import my_jira.catalog.ServiceEntity;
import my_jira.catalog.ServiceRepository;
import my_jira.auth.UsersEntity;
import my_jira.auth.UsersRepo;

@Service
@RequiredArgsConstructor
@Transactional
public class OrdersService {
    private static final String ALLOWED_FORMATS_MESSAGE =
            "Допустимые форматы: PNG, JPG/JPEG, HEIC/HEIF, PDF, DOC, DOCX, DOTX, DOTM, MP4, MOV.";
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/heic",
            "image/heif",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-word.document.macroEnabled.12",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.template",
            "application/vnd.ms-word.template.macroEnabled.12",
            "video/mp4",
            "video/quicktime"
    );
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".png",
            ".jpg",
            ".jpeg",
            ".heic",
            ".heif",
            ".pdf",
            ".doc",
            ".docx",
            ".dotx",
            ".dotm",
            ".mp4",
            ".mov"
    );

    private final OrdersRepo ordersRepo;
    private final UsersRepo usersRepo;
    private final ServiceRepository serviceRepository;
    private final DocumentsRepo documentsRepo;
    @Value("${app.storage.orders-dir:uploads/orders}")
    private String ordersUploadDir;

    public AnswerDto postOrder(OrdersDTO request, String email, List<MultipartFile> documents) {
        Path uploadDir = getUploadDir();
        List<MultipartFile> safeDocuments = documents == null ? Collections.emptyList() : documents;

        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new StorageOperationException("Не удалось создать папку: " + uploadDir, e);
        }

        validateDocuments(safeDocuments);

        UsersEntity user = usersRepo.findByEmail(email)
            .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

        ServiceEntity service = serviceRepository.findByCode(request.getServiceCode());
        if (service == null) {
            throw new ServiceNotFoundException("Услуга не найдена");
        }

        LocalDateTime now = LocalDateTime.now();
        OrdersEntity order = new OrdersEntity();
        order.setClientNameSnapshot(request.getClientName());
        order.setCompanyNameSnapshot(request.getCompanyName());
        order.setContactSnapshot(request.getContact());
        order.setDescription(request.getDescription());
        order.setClientUser(user);
        order.setService(service);
        order.setTitle(service.getName());
        order.setPublicCode("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setStatus("NEW");
        order.setPriority("LOW");
        order.setRevisionCount(0);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        ordersRepo.save(order);

        List<Path> savedFiles = new ArrayList<>();
        List<OrdersDocuments> documentsForSave= new ArrayList<>();
        try {

            for (MultipartFile file : safeDocuments) {
                if (file == null || file.isEmpty()) {
                    continue;
                }

                String originalName = file.getOriginalFilename();
                String contentType = file.getContentType();
                long size = file.getSize();
                String extension = "";
                if (originalName != null && originalName.contains(".")) {
                    extension = originalName.substring(originalName.lastIndexOf("."));
                }

                String storageKey = UUID.randomUUID().toString() + extension;
                Path targetPath = uploadDir.resolve(storageKey);
                try (InputStream inputStream = file.getInputStream()) {
                    // Сохраняем поток явно через NIO: на сервере это стабильнее, чем transferTo(...).
                    Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }

                savedFiles.add(targetPath);

                OrdersDocuments document = new OrdersDocuments();
                document.setUploadedByUser(user);
                document.setOrder(order);
                document.setMimeType(contentType == null ? "application/octet-stream" : contentType);
                document.setSizeBytes(size);
                document.setOriginalFileName(originalName == null ? storageKey : originalName);
                document.setStorageKey(storageKey);
                document.setCreatedAt(now);
                documentsForSave.add(document);
            }
            documentsRepo.saveAll(documentsForSave);
        } catch (IOException error) {
            cleanupSavedFiles(savedFiles);
            throw new StorageOperationException("Не удалось сохранить один из файлов заявки", error);
        } catch (RuntimeException error) {
            cleanupSavedFiles(savedFiles);
            throw error;
        }

        AnswerDto answerDto = new AnswerDto();
        answerDto.setOrderId(order.getId());
        answerDto.setPublicCode(order.getPublicCode());
        answerDto.setStatus(order.getStatus());
        answerDto.setClientName(order.getClientNameSnapshot());
        answerDto.setContact(order.getContactSnapshot());
        answerDto.setCompanyName(order.getCompanyNameSnapshot());
        return answerDto;
    }

    private Path getUploadDir() {
        // Путь к хранилищу конфигурируется снаружи, но сервис остаётся простым для Spring DI.
        return Path.of(ordersUploadDir).toAbsolutePath().normalize();
    }

    private void validateDocuments(List<MultipartFile> documents) {
        List<String> errors = new ArrayList<>();

        for (MultipartFile file : documents) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            String fileName = file.getOriginalFilename() == null
                    ? "безымянный файл"
                    : file.getOriginalFilename();
            if (!isAllowedDocument(file.getContentType(), fileName)) {
                errors.add("Файл \"" + fileName + "\" имеет недопустимый тип.");
            }
        }

        if (!errors.isEmpty()) {
            throw new FileValidationException(
                    "Некоторые файлы не прошли проверку. " + ALLOWED_FORMATS_MESSAGE,
                    errors
            );
        }
    }

    private boolean isAllowedDocument(String contentType, String fileName) {
        String normalizedContentType = contentType == null
                ? ""
                : contentType.trim().toLowerCase(Locale.ROOT);
        if (ALLOWED_CONTENT_TYPES.contains(normalizedContentType)) {
            return true;
        }

        String normalizedFileName = fileName == null ? "" : fileName.trim().toLowerCase(Locale.ROOT);
        int extensionSeparatorIndex = normalizedFileName.lastIndexOf('.');
        if (extensionSeparatorIndex < 0) {
            return false;
        }

        String extension = normalizedFileName.substring(extensionSeparatorIndex);
        return ALLOWED_EXTENSIONS.contains(extension);
    }

    private void cleanupSavedFiles(List<Path> savedFiles) {
        for (Path savedFile : savedFiles) {
            try {
                Files.deleteIfExists(savedFile);
            } catch (IOException ignored) {
                // cleanup не должен скрывать исходную причину ошибки upload-flow
            }
        }
    }
}
