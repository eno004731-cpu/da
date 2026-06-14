package legal_website.dto.verifyemail;

import java.util.UUID;

import lombok.Data;

@Data
public class VerifyEmailPayload {
    private UUID eventId;
    private Long userId;
    private String email;
    private Long verificationCodeId;
    private String purpose;
    private String channel;

    private String link;
}
