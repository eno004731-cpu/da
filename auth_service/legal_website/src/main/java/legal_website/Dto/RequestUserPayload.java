package legal_website.Dto;

import lombok.Data;

@Data
public class RequestUserPayload {
    private Long id;
    private String role;
    private String fullName;
    private String email;
    private String phone;
    private String companyName;
}
