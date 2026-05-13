package my_jira.common.exception;

import java.util.List;

// Ошибка валидации файлов, которую можно безопасно показать пользователю.
public class FileValidationException extends RuntimeException {
    private final List<String> errors;

    public FileValidationException(String message, List<String> errors) {
        super(message);
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}
