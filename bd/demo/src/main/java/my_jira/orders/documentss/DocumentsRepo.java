package my_jira.orders.documentss;


import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import my_jira.orders.order.OrdersEntity;


public interface DocumentsRepo extends JpaRepository<OrdersDocuments,Long> {
    List<OrdersDocuments> findAllByOrder(OrdersEntity order);

    // Ищем конкретный документ только внутри конкретного заказа,
    // чтобы нельзя было скачать чужой файл по одному documentId.
    Optional<OrdersDocuments> findByIdAndOrder(Long id, OrdersEntity order);
    void deleteAllByOrder(OrdersEntity id);
}
