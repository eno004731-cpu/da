package legal_website.dto.login;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Email обязателен")
    @Email(message = "Некорректный формат email")
    private String email;
    @Size(min = 8,max = 72,message = "Пароль должен быть от 8 до 72 символов")
    @NotBlank(message = "Пароль обязателен")
    private String password;
}
