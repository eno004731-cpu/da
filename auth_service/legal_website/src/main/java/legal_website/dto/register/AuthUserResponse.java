package legal_website.dto.register;

import lombok.Data;

@Data
public class AuthUserResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String companyName;
    private String role;
}
