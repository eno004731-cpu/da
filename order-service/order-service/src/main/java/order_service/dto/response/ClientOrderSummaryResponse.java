package order_service.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Data;

@Data
public class ClientOrderSummaryResponse {
    private UUID id;
    private String title;
    private String serviceCode;
    private String serviceName;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int revisionCount;
}
