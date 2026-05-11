package my_jira.common.exception;

// Ошибка для случая, когда заказ не найден
// или недоступен текущему пользователю.
public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(String message) {
        super(message);
    }
}
