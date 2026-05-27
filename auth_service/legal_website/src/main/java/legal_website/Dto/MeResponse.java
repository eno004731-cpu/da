package legal_website.Dto;

import java.util.List;

import legal_website.Dto.register.AuthUserResponse;
import lombok.Data;
@Data
public class MeResponse extends AuthUserResponse{
    
    private List<String> authProviders;
    private Boolean isOAuthUser;
    private Boolean hasPassword;
    private Boolean needsPasswordSetup;
    private Boolean requiresProfileCompletion;

}
