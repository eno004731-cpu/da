package legal_website.dto.verifyemail;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConfirmEmailRequest {
    @NotBlank(message = "токен обязателен")
    private String token;
}
