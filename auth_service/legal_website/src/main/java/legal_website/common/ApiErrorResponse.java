package legal_website.common;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ApiErrorResponse {
    private Integer status;
    private String error;
    private String message;
    private String path;
    private LocalDateTime timestamp;
}
