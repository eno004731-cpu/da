package legal_website.dto;

import lombok.Data;

@Data
public class DeletePayload {
    private Long id;
    private boolean result;
}
