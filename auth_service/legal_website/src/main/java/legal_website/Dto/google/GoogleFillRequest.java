package legal_website.Dto.google;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GoogleFillRequest {
    @NotBlank(message = "flowToken обязателен")
    private String flowToken;
    @NotBlank(message = "Имя обязательно")
    private String fullName;
    @Size(min = 8,max = 72,message = "Пароль должен быть от 8 до 72 символов")
    @NotBlank(message = "Пароль обязателен")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
        message = "Пароль должен содержать букву, цифру и спецсимвол"
    )
    private String password;
}
