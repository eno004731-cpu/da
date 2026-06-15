package order_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateClientOrderRequest {
    @NotBlank
    @Size(max = 100)
    private String serviceCode;

    @NotBlank
    @Size(max = 255)
    private String clientName;

    @NotBlank
    @Size(max = 255)
    private String contact;

    @Size(max = 255)
    private String companyName;

    @NotBlank
    private String description;
}
