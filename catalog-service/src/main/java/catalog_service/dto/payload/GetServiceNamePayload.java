package catalog_service.dto.payload;

import java.util.UUID;

import lombok.Data;

@Data
public class GetServiceNamePayload {
    // Идентификатор заказа, для которого ищем или возвращаем имя услуги.
    private UUID orderId;

    // Код услуги нужен как ключ поиска и для проверки контракта между сервисами.
    private String serviceCode;

    // Человекочитаемое имя услуги, которое catalog-service возвращает в ответ.
    private String serviceName;

    // Идентификатор события помогает сделать обработку идемпотентной.
    private UUID eventId;
}
