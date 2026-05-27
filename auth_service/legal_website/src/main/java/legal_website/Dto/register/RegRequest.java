package legal_website.Dto.register;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegRequest {
    @NotBlank(message = "Имя обязательно")
    private String fullName;
    @NotBlank(message = "Email обязателен")
    @Email(message = "Некорректный формат email")
    private String email;
    private String phone;
    private String companyName;
    @Size(min = 8,max = 72,message = "Пароль должен быть от 8 до 72 символов")
    @NotBlank(message = "Пароль обязателен")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
        message = "Пароль должен содержать букву, цифру и спецсимвол"
    )
    private String password;
}
