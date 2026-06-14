package order_service.persistence.order;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
@Entity
@Table(name = "orders")
@Data
public class OrderEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "client_id",nullable = false)
    private Long clientId;
    @Column(name = "client_name", nullable = false,length = 255)
    private String clientName;
    @Column(name = "contact",nullable = false,length = 255)
    private String contact;
    @Column(name = "company_name", length = 255)
    private String companyName;
    @Column(name = "service_code",length = 100,nullable = false)
    private String serviceCode;
    @Column(name = "service_name",length = 255)
    private String serviceName;
    @Column(name = "title",nullable = false,length = 255)
    private String title;
    @Column(name = "problem_description")
    private String problemDescription;
    @Column(name = "status", length = 30)
    private String status;
    @Column(name = "client_revision_comment")
    private String clientRevisionComment;
    @Column(name = "created_at",nullable = false)
    private LocalDateTime createAt;
    @Column(name = "updated_at",nullable = false)
    private LocalDateTime updatedAt;
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;
}
