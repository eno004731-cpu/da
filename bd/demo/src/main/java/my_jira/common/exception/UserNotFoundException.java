package my_jira.common.exception;

// Ошибка для случая, когда пользователь не найден.
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
