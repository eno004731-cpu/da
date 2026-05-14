package my_jira.orders.order.delOrder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
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
import my_jira.users.UsersEntity;
import my_jira.users.UsersRepo;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderDelService {
    private final UsersRepo usersRepo;
    private final OrdersRepo ordersRepo;
    private final DocumentsRepo documentsRepo;
    public boolean deleteOrder(String email,Long id){
        UsersEntity user = usersRepo.findByEmail(email)
            .orElseThrow(()-> new UserNotFoundException("нет пользователя"));
        
        OrdersEntity order = ordersRepo.findByIdAndClientUser(id,user)
                .orElseThrow(()-> new OrderNotFoundException("нет заказа"));
        List<OrdersDocuments> documents = documentsRepo.findAllByOrder(order);
        deleteFiles(documents);
        documentsRepo.deleteAllByOrder(order);
        ordersRepo.deleteById(id);

        return true;
    }
    @Value("${app.storage.orders-dir:uploads/orders}")
    private String ordersUploadDir;
       private void deleteFiles(List<OrdersDocuments> documents) {
        Path uploadDir = Path.of(ordersUploadDir).toAbsolutePath().normalize();

        for (OrdersDocuments document : documents) {
            Path filePath = uploadDir.resolve(document.getStorageKey());

            try {
                // Удаляем файл, если он существует
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                throw new StorageOperationException(
                        "Не удалось удалить файл заказа: " + document.getStorageKey(),
                        e
                );
            }
        }
    }
}


