package legal_website.Dto.google;

import legal_website.Dto.register.AuthResponse;
import lombok.Data;

@Data
public class GoogleResponse {
    private GoogleFillResponse googleResponse;
    private AuthResponse authResponse;
}
