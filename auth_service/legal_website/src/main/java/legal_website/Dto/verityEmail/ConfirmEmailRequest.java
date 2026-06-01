package legal_website.Dto.verityEmail;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConfirmEmailRequest {
    @NotBlank(message = "токен обязателен")
    private String token;
}
