package legal_website.Dto.google;

import lombok.Data;

@Data
public class GoogleFillResponse {
    private String status;
    private String flowToken;
    private GoogleProfile profile;
}
