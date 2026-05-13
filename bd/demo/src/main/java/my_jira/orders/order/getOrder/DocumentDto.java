package my_jira.orders.order.getOrder;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class DocumentDto {
    private String fileName;
    private Long size;
    private LocalDateTime uploadedAt;
    private String downloadUrl;
    private boolean isDeleted;


}
