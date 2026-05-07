package my_jira.users.Me;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@RequiredArgsConstructor
public class UserResponse {
//     "id": 1,
//   "fullName": "Иван Иванов",
//   "email": "ivan@test.ru",
//   "phone": "+7999...",
//   "companyName": "ООО Ромашка",
//   "role": "CLIENT"
    @NotBlank
    private Long id;
    private String fullName;
    @Email
    @NotBlank
    private String email;
    private String phone;
    private String companyName;
    private String role;

}
