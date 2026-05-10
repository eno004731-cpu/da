package my_jira.common.exception;

// Ошибка для проблем при работе с файловым хранилищем.
public class StorageOperationException extends RuntimeException {
    public StorageOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
