package my_jira.orders.order.postEnpoint;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import my_jira.common.exception.ServiceNotFoundException;
import my_jira.common.exception.StorageOperationException;
import my_jira.common.exception.UserNotFoundException;
import my_jira.orders.documentss.DocumentsRepo;
import my_jira.orders.documentss.OrdersDocuments;
import my_jira.orders.order.OrdersEntity;
import my_jira.orders.order.OrdersRepo;
import my_jira.services.ServiceEntity;
import my_jira.services.ServiceRepository;
import my_jira.users.UsersEntity;
import my_jira.users.UsersRepo;

@Service
@RequiredArgsConstructor
@Transactional
public class OrdersService {
    private final Path uploadDir;
    private final OrdersRepo ordersRepo;
    private final UsersRepo usersRepo;
    private final ServiceRepository serviceRepository;
    private final DocumentsRepo documentsRepo;

    public OrdersService(
            @Value("${app.storage.orders-dir:uploads/orders}") String ordersUploadDir,
            OrdersRepo ordersRepo,
            UsersRepo usersRepo,
            ServiceRepository serviceRepository,
            DocumentsRepo documentsRepo
    ) {
        // Нормализуем путь один раз, чтобы upload и download работали с одинаковым расположением файлов.
        this.uploadDir = Path.of(ordersUploadDir).toAbsolutePath().normalize();
        this.ordersRepo = ordersRepo;
        this.usersRepo = usersRepo;
        this.serviceRepository = serviceRepository;
        this.documentsRepo = documentsRepo;
    }

    public AnswerDto postOrder(OrdersDTO request, String email, List<MultipartFile> documents) {
        List<MultipartFile> safeDocuments = documents == null ? Collections.emptyList() : documents;

        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new StorageOperationException("Не удалось создать папку: " + uploadDir, e);
        }

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
            } catch (IOException error) {
                throw new StorageOperationException("Не удалось сохранить файл " + targetPath, error);
            }

            OrdersDocuments document = new OrdersDocuments();
            document.setUploadedByUser(user);
            document.setOrder(order);
            document.setMimeType(contentType == null ? "application/octet-stream" : contentType);
            document.setSizeBytes(size);
            document.setOriginalFileName(originalName == null ? storageKey : originalName);
            document.setStorageKey(storageKey);
            document.setCreatedAt(now);
            documentsRepo.save(document);


        }
        AnswerDto answerDto = new AnswerDto();
        answerDto.setOrderId(order.getId());
        answerDto.setPublicCode(order.getPublicCode());
        answerDto.setStatus(order.getStatus());
        return answerDto;
    }
}
