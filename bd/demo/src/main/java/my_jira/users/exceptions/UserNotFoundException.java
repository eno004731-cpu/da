package my_jira.users.exceptions;

// Это исключение для ситуации, когда сущность не найдена
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }
}