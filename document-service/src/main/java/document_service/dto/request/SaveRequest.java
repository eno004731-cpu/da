package document_service.dto.request;

import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

@Data
public class SaveRequest {
    private UUID orderId;
    // Идентификатор уже извлечён из проверенного JWT на уровне security-фильтра.
    private Long userId;
    private List<MultipartFile> documents;
}
