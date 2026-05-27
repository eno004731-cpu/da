package legal_website.common.errors.OAuht;

public class OAuthAccountNotFoundException extends RuntimeException {
    public OAuthAccountNotFoundException(String message) {
        super(message);
    }
}
