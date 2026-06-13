package order_service.Services.orders;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import order_service.Dto.payload.GetServiceNamePayload;
import order_service.Dto.request.CreateOrderRequest;
import order_service.Dto.response.CreateOrderResponse;
import order_service.EntityAndRepo.events.outbox.OutboxEventEntity;
import order_service.EntityAndRepo.events.outbox.OutboxEventRepo;
import order_service.EntityAndRepo.order.OrderEntity;
import order_service.EntityAndRepo.order.OrderRepo;
import order_service.Services.events.EventStatusService;
import order_service.Services.events.OutboxWakeUpEvent;

@Service
@RequiredArgsConstructor
public class CreateOrderService {
    private final OrderRepo orderRepo;
    private final ObjectMapper objectMapper;
    private final EventStatusService eventStatusService;
    private final OutboxEventRepo eventRepo;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest request,Long id){
        OrderEntity order = createOrderEntity(request, id);
        orderRepo.save(order);

        OutboxEventEntity event = new OutboxEventEntity();
        // eventId нужен до сериализации payload, чтобы outbox и payload ссылались на одно событие.
        event.setId(UUID.randomUUID());
        GetServiceNamePayload payload = new GetServiceNamePayload();
        payload.setServiceCode(order.getServiceCode());
        payload.setOrderId(order.getId());
        payload.setEventId(event.getId());
        event.setAggregateId(order.getId());
        event.setCreatedAt(LocalDateTime.now());
        event.setRetryCount(0);
        event.setEventType("SERVICE_NAME_REQUESTED");  
        try {
            event.setPayload(objectMapper.valueToTree(payload));
        } catch (Exception e) {
            eventStatusService.saveFailedEvent(event, e.toString());
            throw new IllegalStateException("Не удалось подготовить outbox-событие для запроса названия услуги", e);
        }
        event.setStatus("NEW");
        eventRepo.save(event);
        // Будим publisher только после коммита транзакции, чтобы он видел уже сохранённую запись outbox.
        applicationEventPublisher.publishEvent(new OutboxWakeUpEvent(event.getId()));

        CreateOrderResponse response = new CreateOrderResponse();
        response.setId(order.getId());
        response.setOrderId(order.getId());
        response.setStatus(order.getStatus());
        return response;
    }

    private OrderEntity createOrderEntity(CreateOrderRequest request,Long id){
        String description = request.getDescription().trim();
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setClientId(id);
        orderEntity.setClientName(request.getClientName());
        orderEntity.setCompanyName(request.getCompanyName());
        orderEntity.setContact(request.getContact());
        orderEntity.setServiceCode(request.getServiceCode());
        // orderEntity.setServiceName(); нужно сделать микросервис и здесь тогда запрос или рядом бд
        // В title держим короткий человекочитаемый заголовок, а полное описание отдельно.
        orderEntity.setTitle(buildTitle(description));
        orderEntity.setProblemDescription(description);
        orderEntity.setStatus("ON_REVIEW");
        orderEntity.setCreateAt(LocalDateTime.now());
        orderEntity.setUpdatedAt(LocalDateTime.now());
        return orderEntity;
    }

    private String buildTitle(String description) {
        if (description.length() <= 255) {
            return description;
        }
        return description.substring(0, 252) + "...";
    }
}
