package my_jira.orders.order;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import my_jira.orders.documentss.DocumentsRepo;
import my_jira.orders.documentss.OrdersDocuments;
import my_jira.services.ServiceEntity;
import my_jira.services.ServiceRepository;
import my_jira.users.UsersEntity;
import my_jira.users.UsersRepo;

@Service
@RequiredArgsConstructor
@Transactional
public class OrdersService {
    private final OrdersRepo ordersRepo;
    private final UsersRepo usersRepo;
    private final ServiceRepository serviceRepository;
    private final DocumentsRepo documentsRepo;

    public AnswerDto postOrder(OrdersDTO request, String email, List<MultipartFile> documents) {
        Path uploadDir = Path.of("uploads", "orders");
        List<MultipartFile> safeDocuments = documents == null ? Collections.emptyList() : documents;

        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать папку: " + uploadDir, e);
        }

        UsersEntity user = usersRepo.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        ServiceEntity service = serviceRepository.findByCode(request.getServiceCode());
        if (service == null) {
            throw new RuntimeException("Услуга не найдена");
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
            try {
                file.transferTo(targetPath.toFile());
            } catch (IOException error) {
                throw new RuntimeException("Не удалось сохранить файл " + targetPath, error);
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
        answerDto.setOrderId(user.getId());
        answerDto.setPublicCode(order.getPublicCode());
        answerDto.setStatus(order.getStatus());
        return answerDto;
    }
}
