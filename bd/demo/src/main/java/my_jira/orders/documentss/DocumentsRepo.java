package my_jira.orders.documentss;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentsRepo extends JpaRepository<OrdersDocuments,Long> {
    
}
