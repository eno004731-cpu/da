package legal_website.dto;

import java.util.List;

import legal_website.dto.register.AuthUserResponse;
import lombok.Data;
@Data
public class MeResponse extends AuthUserResponse{
    
    private List<String> authProviders;
    private Boolean isOAuthUser;
    private Boolean hasPassword;
    private Boolean needsPasswordSetup;
    private Boolean requiresProfileCompletion;

}
