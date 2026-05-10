package my_jira.common.exception;

// Ошибка для случая, когда нужная услуга не найдена.
public class ServiceNotFoundException extends RuntimeException {
    public ServiceNotFoundException(String message) {
        super(message);
    }
}
