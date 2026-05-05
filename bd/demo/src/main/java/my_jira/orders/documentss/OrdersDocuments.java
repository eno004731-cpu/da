package my_jira.orders.documentss;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import jakarta.persistence.Table;
import my_jira.orders.order.OrdersEntity;
import my_jira.users.UsersEntity;

@Entity
@Table(name = "orders_documents")
public class OrdersDocuments {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id",nullable = false)
    private OrdersEntity order ;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_user_id",nullable = false)
    private UsersEntity uploadedByUser;
    @Column(name = "original_file_name",length = 255,nullable = false)
    private String originalFileName;
    @Column(name = "storage_key",length = 255,nullable = false)

    private String storageKey ;
    @Column(name = "mime_type",length = 100,nullable = false)
    private String mimeType;
    @Column(name = "size_bytes")
    private Long sizeBytes ;
    @Column(name = "is_deleted")
    private boolean isDeleted;
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    @Column(name = "created_at")
    private LocalDateTime createdAt ;

    public void setOrder(OrdersEntity order) {
        this.order = order;
    }

    public void setUploadedByUser(UsersEntity uploadedByUser) {
        this.uploadedByUser = uploadedByUser;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
