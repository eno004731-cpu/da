package my_jira.common.exception;

// Ошибка для случаев, когда запрос пришёл без валидной аутентификации.
public class AuthenticationRequiredException extends RuntimeException {
    public AuthenticationRequiredException(String message) {
        super(message);
    }
}
