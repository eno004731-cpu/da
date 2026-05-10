package my_jira.common.exception;

import java.time.LocalDateTime;

// Единый формат ошибки для REST API.
public record ApiErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
