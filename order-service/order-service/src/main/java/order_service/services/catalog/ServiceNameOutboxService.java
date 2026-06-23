package order_service.services.catalog;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import order_service.dto.payload.GetServiceNamePayload;
import order_service.persistence.events.outbox.OutboxEventEntity;
import order_service.persistence.events.outbox.OutboxEventRepo;
import order_service.persistence.order.OrderEntity;
import order_service.services.events.outbox.EventStatusService;
import order_service.services.events.outbox.OutboxWakeUpEvent;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceNameOutboxService {
    private final ObjectMapper objectMapper;
    private final EventStatusService eventStatusService;
    private final OutboxEventRepo eventRepo;
    private final ApplicationEventPublisher applicationEventPublisher;

    public void createServiceNameRequestedEvent(OrderEntity order) {
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
        // Логируем только технические идентификаторы, без данных клиента.
        log.info("Service name outbox event created eventId={} orderId={} serviceCode={}",
                event.getId(), order.getId(), order.getServiceCode());
        // Будим publisher только после коммита транзакции, чтобы он видел уже сохранённую запись outbox.
        applicationEventPublisher.publishEvent(new OutboxWakeUpEvent(event.getId()));
    }
}
