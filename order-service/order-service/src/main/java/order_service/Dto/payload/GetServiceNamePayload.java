package order_service.Dto.payload;

import java.util.UUID;

import lombok.Data;

@Data
public class GetServiceNamePayload {
    // Заказ, для которого catalog-service нашёл человекочитаемое имя услуги.
    private UUID orderId;

    // Код услуги полезно хранить в ответе для дополнительной валидации контракта.
    private String serviceCode;

    // Это значение потом записываем в orders.service_name.
    private String serviceName;
    private UUID eventId;
}
