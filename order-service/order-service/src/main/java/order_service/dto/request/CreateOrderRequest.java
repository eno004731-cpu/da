package order_service.dto.request;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateOrderRequest {
    @NotBlank
    private String serviceCode;
    @NotBlank
    private String clientName;
    @NotBlank
    private String contact;
    private String companyName;
    @NotBlank
    private String description;
    
}
