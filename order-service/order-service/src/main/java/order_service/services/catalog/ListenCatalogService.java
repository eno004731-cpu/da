package order_service.services.catalog;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import order_service.dto.payload.GetServiceNamePayload;
import order_service.persistence.events.incoming.IncomingEventEntity;
import order_service.persistence.events.incoming.IncomingEventRepo;
import order_service.persistence.events.incoming.IncomingEventEntity.Status;
import order_service.persistence.order.OrderEntity;
import order_service.persistence.order.OrderRepo;
import order_service.services.events.outbox.EventStatusService;

@Service
@RequiredArgsConstructor
@Slf4j
public class ListenCatalogService {
    private final IncomingEventRepo incomingEventRepo;
    private final OrderRepo orderRepo;
    private final ObjectMapper objectMapper;
    private final EventStatusService eventStatusService;

    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroup;

    @Transactional
    public void handleCatalogResponse(ConsumerRecord<String, GetServiceNamePayload> record) {
        GetServiceNamePayload payload = record.value();
        if (payload == null) {
            log.warn("Catalog response skipped because payload is null topic={} partition={} offset={}",
                    record.topic(), record.partition(), record.offset());
            return;
        }

        UUID eventId = payload.getEventId();
        if (incomingEventRepo.existsByEventId(eventId)) {
            log.info("Catalog response skipped because event already processed eventId={}", eventId);
            return;
        }

        IncomingEventEntity incomingEvent = saveReceivedEvent(record, payload, eventId);

        if (payload.getOrderId() == null) {
            eventStatusService.saveDeadIncomingEvent(incomingEvent, "В ответе catalog-service отсутствует orderId");
            return;
        }

        if (payload.getServiceName() == null || payload.getServiceName().isBlank()) {
            eventStatusService.saveDeadIncomingEvent(incomingEvent, "В ответе catalog-service отсутствует serviceName");
            return;
        }

        Optional<OrderEntity> orderOptional = orderRepo.findById(payload.getOrderId());
        if (orderOptional.isEmpty()) {
            eventStatusService.saveDeadIncomingEvent(incomingEvent, "Заказ для ответа catalog-service не найден");
            return;
        }

        OrderEntity order = orderOptional.get();
        order.setServiceName(payload.getServiceName());
        order.setUpdatedAt(LocalDateTime.now());
        orderRepo.save(order);

        eventStatusService.saveProcessedIncomingEvent(incomingEvent);
    }

    private IncomingEventEntity saveReceivedEvent(
            ConsumerRecord<String, GetServiceNamePayload> record,
            GetServiceNamePayload payload,
            UUID eventId
    ) {
        IncomingEventEntity incomingEvent = new IncomingEventEntity();
        incomingEvent.setEventId(eventId);
        incomingEvent.setTopic(record.topic());
        incomingEvent.setPartitionNo(record.partition());
        incomingEvent.setMessageOffset(record.offset());
        incomingEvent.setConsumerGroup(consumerGroup);
        incomingEvent.setAggregateId(payload.getOrderId());
        incomingEvent.setEventType("SERVICE_NAME_RESOLVED");
        incomingEvent.setPayload(objectMapper.valueToTree(payload));
        incomingEvent.setStatus(Status.RECEIVED);
        incomingEvent.setRetryCount(0);
        incomingEvent.setReceivedAt(LocalDateTime.now());
        incomingEventRepo.save(incomingEvent);
        return incomingEvent;
    }
}
