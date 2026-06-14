package legal_website.dto.refreshtoken;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data

public class TokenRequest {
    @NotBlank(message = "Refresh token обязателен")
    private String refreshToken;
}
