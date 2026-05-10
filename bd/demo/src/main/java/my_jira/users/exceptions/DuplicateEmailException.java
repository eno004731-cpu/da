package my_jira.users.exceptions;



// Это исключение для ситуации, когда email уже занят
public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String message) {
        super(message);
    }
}
