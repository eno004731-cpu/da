package legal_website.Dto.verityEmail;

import lombok.Data;

@Data
public class VerityEmailResponse {
    private String status;
    private String channel;
    private String recipientMasked;
    private Integer expiresInSeconds;

}
