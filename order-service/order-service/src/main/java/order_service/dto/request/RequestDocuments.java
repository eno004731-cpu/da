package order_service.dto.request;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RequestDocuments {
    @NotBlank
    private List<MultipartFile> documents;
}
