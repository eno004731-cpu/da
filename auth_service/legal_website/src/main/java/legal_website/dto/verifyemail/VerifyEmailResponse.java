package legal_website.dto.verifyemail;

import lombok.Data;

@Data
public class VerifyEmailResponse {
    private String status;
    private String channel;
    private String recipientMasked;
    private Integer expiresInSeconds;

}
