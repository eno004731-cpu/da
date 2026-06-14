package legal_website.common.errors.oauth;

public class OAuthProviderUnavailableException extends RuntimeException {
    public OAuthProviderUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
