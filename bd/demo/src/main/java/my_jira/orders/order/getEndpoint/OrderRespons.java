package my_jira.orders.order.getEndpoint;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderRespons {
    private Long id;
    private String title;
    private String status;
    private String serviceName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer revisionCount;
    private String problemDescription;
    private List<DocumentDto> documents;
    private String clientRevisionComment;
    private LocalDateTime clientRevisionRequestedAt;
}
