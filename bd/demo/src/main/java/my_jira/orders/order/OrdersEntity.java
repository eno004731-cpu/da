package my_jira.orders.order;

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
import lombok.Getter;
import my_jira.services.ServiceEntity;
import my_jira.users.UsersEntity;

@Entity
@Table(name = "orders")
@Getter
public class OrdersEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "public_code", nullable = false,length = 100,unique = true)
    private String publicCode;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_user_id", nullable = false)
    private UsersEntity clientUser; 
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceEntity service ;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_user_id")
    private UsersEntity assignedToUser;

    @Column(name="title",length = 255,nullable = false)
    private String title ;
    @Column(name = "description")
    private String description;
    @Column(name = "status",nullable = false,length = 50)
    private String status;
    @Column(name = "priority",nullable = false,length = 50)
    private String priority;
    @Column(name = "client_name_snapshot",length = 255)
    private String clientNameSnapshot ;
    @Column(name = "contact_snapshot")
    private String contactSnapshot ;
    @Column(name="company_name_snapshot",length = 255)
    private String companyNameSnapshot;
    @Column(name = "client_revision_comment")
    private String clientRevisionComment ;
    @Column(name = "client_revision_requested_at")
    private LocalDateTime clientRevisionRequestedAt;
    @Column(name = "revision_count")
    private Integer revisionCount;
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    @Column(name = "created_at",nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at",nullable = false)
    private LocalDateTime updatedAt ;

    public void setPublicCode(String publicCode) {
        this.publicCode = publicCode;
    }

    public void setClientUser(UsersEntity clientUser) {
        this.clientUser = clientUser;
    }

    public void setService(ServiceEntity service) {
        this.service = service;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public void setClientNameSnapshot(String clientNameSnapshot) {
        this.clientNameSnapshot = clientNameSnapshot;
    }

    public void setContactSnapshot(String contactSnapshot) {
        this.contactSnapshot = contactSnapshot;
    }

    public void setCompanyNameSnapshot(String companyNameSnapshot) {
        this.companyNameSnapshot = companyNameSnapshot;
    }

    public void setRevisionCount(Integer revisionCount) {
        this.revisionCount = revisionCount;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
