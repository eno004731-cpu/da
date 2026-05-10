package my_jira.users.exceptions;


import java.time.LocalDateTime;

// Единый формат ошибки, который удобно читать и фронту, и тебе
public record ApiErrorResponse(
        int status,
        String error,
        String message,
        LocalDateTime timestamp
) {
}