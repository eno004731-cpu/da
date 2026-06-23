package order_service.services.catalog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import order_service.dto.payload.GetServiceNamePayload;
import order_service.persistence.events.outbox.OutboxEventEntity;
import order_service.persistence.events.outbox.OutboxEventRepo;
import order_service.persistence.order.OrderEntity;
import order_service.persistence.order.OrderRepo;
import order_service.services.events.outbox.EventStatusService;

@Service
@RequiredArgsConstructor
@Slf4j
public class SendEventForGetServiceName {
    private static final String EVENT_TYPE = "SERVICE_NAME_REQUESTED";

    private final ObjectMapper objectmapper;
    private final OutboxEventRepo eventRepo;
    private final KafkaTemplate<String,GetServiceNamePayload> kafkaTemplateCatalog;
    private final EventStatusService eventStatusService;
    private final OrderRepo orderRepo;
    private final AtomicBoolean processing = new AtomicBoolean(false);

    @Value("${app.kafka.topics.catalog-get-service-name-request}")
    private String requestTopic;

    @Scheduled(fixedDelay = 5000)
    public void sendEvent(){
        processAvailableEvents();
    }

    public void processAvailableEvents() {
        // Не даём scheduler и wake-up listener одновременно гонять один и тот же polling.
        if (!processing.compareAndSet(false, true)) {
            log.debug("Catalog outbox polling skipped because another polling cycle is running");
            return;
        }

        try {
            List<OutboxEventEntity> newEvents =
                    eventRepo.findTop100ByStatusAndEventTypeOrderByCreatedAtAsc("NEW", EVENT_TYPE);
            List<OutboxEventEntity> failedEvents = eventRepo
                    .findTop100ByStatusAndEventTypeAndNextRetryAtBeforeOrderByNextRetryAtAsc(
                            "FAILED",
                            EVENT_TYPE,
                            LocalDateTime.now()
                    );

            // Размер пачки помогает диагностировать backlog, не раскрывая содержимое событий.
            if (!newEvents.isEmpty() || !failedEvents.isEmpty()) {
                log.debug("Catalog outbox events found newCount={} retryCount={} topic={}",
                        newEvents.size(), failedEvents.size(), requestTopic);
            }
            sendEvents(newEvents);
            sendEvents(failedEvents);
        } finally {
            processing.set(false);
        }
    }

    private void sendEvents(List<OutboxEventEntity> events){
        for (OutboxEventEntity event : events) {
            GetServiceNamePayload payload = getPayload(event);
            if (payload == null) {
                continue;
            }
            if (payload.getServiceCode() == null || payload.getServiceCode().isBlank()) {
                eventStatusService.saveDeadEvent(event, "пустой ServiceCode");
                continue;
            }
            Optional<OrderEntity> order = orderRepo.findById(event.getAggregateId());
            if (order.isEmpty()) {
                eventStatusService.saveDeadEvent(event, "нет заказа");
                continue;
            }
            
            sendMessage(event, payload);
        }
    }
    private GetServiceNamePayload getPayload(OutboxEventEntity event ){
        GetServiceNamePayload payload;
        try {
            payload = objectmapper.treeToValue(event.getPayload(), GetServiceNamePayload.class);
            return payload;
        } catch (Exception e) {
            eventStatusService.saveFailedEvent(event, e.toString());
            return null;
        }
    }
    private void sendMessage(OutboxEventEntity event,GetServiceNamePayload payload){
        try {
            kafkaTemplateCatalog.send(requestTopic,event.getAggregateId().toString(),payload).whenComplete(
                (result,error)->{
                    if (error == null) {
                        eventStatusService.savePublishedEvent(event);
                    } else {
                        eventStatusService.saveFailedEvent(event, error.toString());
                    }
                }
            );
        } catch (Exception e) {
           eventStatusService.saveFailedEvent(event, e.toString());
        }
    }

}
