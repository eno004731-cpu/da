package my_jira.orders.order.getEndpoint;

import java.net.MalformedURLException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import my_jira.common.exception.OrderNotFoundException;
import my_jira.common.exception.StorageOperationException;
import my_jira.common.exception.UserNotFoundException;
import my_jira.orders.documentss.DocumentsRepo;
import my_jira.orders.documentss.OrdersDocuments;
import my_jira.orders.order.OrdersEntity;
import my_jira.orders.order.OrdersRepo;
import my_jira.services.ServiceEntity;
import my_jira.users.UsersEntity;
import my_jira.users.UsersRepo;

@Service
@RequiredArgsConstructor
public class OrdersGetService {

    private final Path uploadDir;
    private final OrdersRepo ordersRepo;
    private final UsersRepo usersRepo;
    private final DocumentsRepo documentsRepo;

    public OrdersGetService(
            @Value("${app.storage.orders-dir:uploads/orders}") String ordersUploadDir,
            OrdersRepo ordersRepo,
            UsersRepo usersRepo,
            DocumentsRepo documentsRepo
    ) {
        // Используем тот же конфиг, что и при загрузке, чтобы скачивание смотрело туда же.
        this.uploadDir = Path.of(ordersUploadDir).toAbsolutePath().normalize();
        this.ordersRepo = ordersRepo;
        this.usersRepo = usersRepo;
        this.documentsRepo = documentsRepo;
    }

    @Transactional(readOnly = true)
    public OrderRespons getOrder(Long id,String email){
        OrdersEntity order = getOwnedOrder(id, email);

        OrderRespons respons = new OrderRespons();
        respons.setId(order.getId());
        respons.setTitle(order.getTitle());
        respons.setStatus(order.getStatus());
        ServiceEntity service = order.getService();
        respons.setServiceName(service.getName());
        // В ответе нужна дата создания именно заказа, а не услуги.
        respons.setCreatedAt(order.getCreatedAt());
        respons.setUpdatedAt(order.getUpdatedAt());
        respons.setRevisionCount(order.getRevisionCount());
        respons.setClientRevisionComment(order.getClientRevisionComment());
        respons.setProblemDescription(order.getDescription());
        respons.setClientRevisionRequestedAt(order.getClientRevisionRequestedAt());
        List<OrdersDocuments> documents = documentsRepo.findAllByOrder(order);
        List<DocumentDto> documentsObjects = new ArrayList<>();
        for(OrdersDocuments documet:documents){
            DocumentDto documentDto = new DocumentDto();
            documentDto.setFileName(documet.getOriginalFileName());
            documentDto.setSize(documet.getSizeBytes());
            // Фронту отдаём не внутренний storageKey, а контролируемый backend URL.
            documentDto.setDownloadUrl(buildDownloadUrl(order.getId(), documet.getId()));
            documentDto.setUploadedAt(documet.getCreatedAt());
            documentDto.setDeleted(documet.isDeleted());
            
            
            documentsObjects.add(documentDto);
        }
        respons.setDocuments(documentsObjects);

        return respons;
    }

    public ResponseEntity<Resource> downloadDocument(Long orderId, Long documentId, String email) {
        OrdersEntity order = getOwnedOrder(orderId, email);

        OrdersDocuments document = documentsRepo.findByIdAndOrder(documentId, order)
                .orElseThrow(() -> new OrderNotFoundException("Документ не найден"));

        if (document.isDeleted()) {
            throw new OrderNotFoundException("Документ не найден");
        }

        Path filePath = uploadDir.resolve(document.getStorageKey()).normalize();
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new StorageOperationException("Файл документа не найден в хранилище", null);
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());
            String mimeType = resolveMimeType(filePath, document.getMimeType());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(mimeType))
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.inline()
                                    .filename(document.getOriginalFileName())
                                    .build()
                                    .toString()
                    )
                    .body(resource);
        } catch (MalformedURLException error) {
            throw new StorageOperationException("Не удалось подготовить документ к скачиванию", error);
        }
    }

    private OrdersEntity getOwnedOrder(Long orderId, String email) {
        // Ищем текущего пользователя по email из сессии.
        UsersEntity user = usersRepo.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

        // Проверяем, что заказ принадлежит текущему пользователю.
        // Если нет, возвращаем "не найден", чтобы не раскрывать чужие id.
        List<Long> idsOrders = ordersRepo.findAllIdsByClientUser(user);
        if (!idsOrders.contains(orderId)) {
            throw new OrderNotFoundException("Заказ не найден");
        }

        return ordersRepo.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Заказ не найден"));
    }

    private String buildDownloadUrl(Long orderId, Long documentId) {
        return "/api/client/orders/" + orderId + "/documents/" + documentId + "/download";
    }

    private String resolveMimeType(Path filePath, String storedMimeType) {
        try {
            String detectedMimeType = Files.probeContentType(filePath);
            if (detectedMimeType != null && !detectedMimeType.isBlank()) {
                return detectedMimeType;
            }
        } catch (Exception ignored) {
            // Если ОС не смогла определить content type, используем запасной вариант ниже.
        }

        if (storedMimeType != null && !storedMimeType.isBlank()) {
            return storedMimeType;
        }

        String guessedMimeType = URLConnection.guessContentTypeFromName(filePath.getFileName().toString());
        return guessedMimeType != null ? guessedMimeType : MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
}
