package legal_website.common.errors.oauth;

public class OAuthAccountNotFoundException extends RuntimeException {
    public OAuthAccountNotFoundException(String message) {
        super(message);
    }
}
