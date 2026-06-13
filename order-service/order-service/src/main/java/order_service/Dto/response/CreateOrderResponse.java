package order_service.Dto.response;

import java.util.UUID;

import lombok.Data;

@Data
public class CreateOrderResponse {
    private UUID id;
    private UUID orderId;
    private String status;
}
