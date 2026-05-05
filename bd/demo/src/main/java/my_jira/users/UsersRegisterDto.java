package my_jira.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class UsersRegisterDto {
    @NotBlank
    private String fullName;
    @NotBlank
    @Email
    private String email;
    private String phone;
    private String companyName;
    @NotBlank
    private String password;

    UsersRegisterDto() {
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getPassword() {
        return password;
    }
}
