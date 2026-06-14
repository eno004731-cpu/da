package order_service.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.Data;

@Data
public class ClientOrderDetailsResponse {
    private UUID id;
    private String title;
    private String serviceCode;
    private String serviceName;
    private String clientName;
    private String contact;
    private String companyName;
    private String problemDescription;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int revisionCount;
    private List<UploadedDocumentResponse> documents;
}
