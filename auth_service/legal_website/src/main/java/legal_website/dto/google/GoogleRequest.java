package legal_website.dto.google;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleRequest {
    @NotBlank(message = "Google credential обязателен")
    private String credential;
}
