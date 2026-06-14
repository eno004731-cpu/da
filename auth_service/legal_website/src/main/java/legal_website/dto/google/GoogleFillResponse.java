package legal_website.dto.google;

import lombok.Data;

@Data
public class GoogleFillResponse {
    private String status;
    private String flowToken;
    private GoogleProfile profile;
}
