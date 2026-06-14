package legal_website.dto.google;

import legal_website.dto.register.AuthResponse;
import lombok.Data;

@Data
public class GoogleResponse {
    private GoogleFillResponse googleResponse;
    private AuthResponse authResponse;
}
