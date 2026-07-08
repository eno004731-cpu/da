package lawyer_service.dto;

import lombok.Data;

@Data

public class AuthRequest {
    private String password;
    private String email;
    private String phone;
    private String firstName;
    private String lastName;
    private String middleName;
    private String Specialization;
    private String ip;
    private String agentId;
}
