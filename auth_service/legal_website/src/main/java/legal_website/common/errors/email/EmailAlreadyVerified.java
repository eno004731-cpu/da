package legal_website.common.errors.email;

public class EmailAlreadyVerified extends RuntimeException {
    public EmailAlreadyVerified(String message) {
        super(message);
    }
    
}
