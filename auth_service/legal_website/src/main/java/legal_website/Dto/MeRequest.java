package legal_website.Dto;

import lombok.Data;


// это для PATCH запроса
@Data
public class MeRequest {
    private String fullName;
    private String companyName;
    private String NewPassword;
    private String CurrentPassword;
}
