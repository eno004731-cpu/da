package legal_website.common.errors.oauth;

public class OAuthAccountAlreadyLinkedException extends RuntimeException {
    public OAuthAccountAlreadyLinkedException(String message) {
        super(message);
    }
    
}
