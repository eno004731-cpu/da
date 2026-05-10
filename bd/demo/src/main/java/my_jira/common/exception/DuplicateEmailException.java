package my_jira.common.exception;

// Ошибка для конфликта уникальности email.
public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String message) {
        super(message);
    }
}
