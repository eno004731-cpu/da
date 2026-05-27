package legal_website.Dto.google;

import lombok.Data;

@Data
public class GoogleFlowDto {
    private String sub;
    private String type;
    private String provider;
    private String email;
    private boolean emailVerified;
    private String fullName;
    
}
