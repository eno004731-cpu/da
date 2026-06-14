package order_service.dto.response;

import java.util.UUID;

import lombok.Data;

@Data
public class CreateOrderResponse {
    private UUID id;
    private UUID orderId;
    private String status;
}
