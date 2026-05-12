package my_jira.orders.order.getOrders;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ClientOrders {
    // id заказа, нужен для перехода в details page
    private Long id;

    // заголовок заказа
    private String title;

    // код услуги, фронт его ожидает по контракту
    private String serviceCode;

    // человекочитаемое название услуги
    private String serviceName;

    // статус заказа: NEW / IN_PROGRESS / DONE ...
    private String status;

    // дата создания заказа
    private LocalDateTime createdAt;

    // дата последнего изменения
    private LocalDateTime updatedAt;

    // сколько раз клиент отправлял на доработку
    private Integer revisionCount;
}
