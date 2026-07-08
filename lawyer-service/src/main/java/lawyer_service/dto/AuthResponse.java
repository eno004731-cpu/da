package lawyer_service.dto;

import java.time.Instant;
import java.util.UUID;

import lawyer_service.repo_entity.enums.Role;
import lawyer_service.repo_entity.enums.StatusLawyer;
import lombok.Data;

@Data
public class AuthResponse {
    private UUID laweyr;
    private LawyerInformation lawyerInformation;
    private Role role;
    private StatusLawyer status;
    private Instant lastLoginAt;
    private Instant createdAt;
    private Instant updatedAt;
    private String refreshToken;
    private String accessToken;
}
