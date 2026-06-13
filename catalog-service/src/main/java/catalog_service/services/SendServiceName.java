package catalog_service.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import catalog_service.dto.payload.GetServiceNamePayload;
import catalog_service.entityAndRepo.outbox.OutboxEventEntity;
import catalog_service.entityAndRepo.outbox.OutboxEventRepo;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SendServiceName {
    private final OutboxEventRepo outboxEventRepo;
    private final ObjectMapper objectMapper;
    private final EventService eventService;
    private final KafkaTemplate<String,GetServiceNamePayload> kafkaTemplate;
    private final AtomicBoolean processing = new AtomicBoolean();

    @Value("${app.kafka.topics.catalog-get-service-name-response}")
    private String responseTopic;

    @Scheduled(fixedDelay = 5000)
    public void sendEvent1(){
        sendEvent();
    }

    public void sendEvent(){
        if (!processing.compareAndSet(false, true)) {
            return;
        }
        try {
            List< OutboxEventEntity> newEvent = outboxEventRepo.findTop100ByStatusOrderByCreatedAtAsc("NEW");
            List<OutboxEventEntity> failedEvents = outboxEventRepo.findTop100ByStatusAndNextRetryAtBeforeOrderByNextRetryAtAsc("FAILED", LocalDateTime.now());
            sendEvents(newEvent);
            sendEvents(failedEvents);
        } finally{
            processing.set(false);
        }
    }
    private void sendEvents(List<OutboxEventEntity> events){
        for (OutboxEventEntity event : events) {
            GetServiceNamePayload payload= getPatload(event);
            if (payload == null) {
                continue;
            }
            if (payload.getServiceName() == null) {
                eventService.saveDeadEvent(event, "ServiceName == null");
                continue;
            }
            payload.setEventId(event.getId());
            try {
                kafkaTemplate.send(responseTopic, event.getAggregateId().toString(),payload).whenComplete(
                    (result,error)->{
                        if (error == null) {
                            eventService.savePublished(event);
                        } else {
                           eventService.saveFailedEvent(event, error.toString()); 
                        }
                    }
                );
            } catch (Exception e) {
                eventService.saveFailedEvent(event, e.toString());
            }
        }
    }
    private GetServiceNamePayload getPatload(OutboxEventEntity event){
        GetServiceNamePayload payload = null;
        try {
            payload = objectMapper.treeToValue(event.getPayload(), GetServiceNamePayload.class);
            return payload;
        } catch (Exception e) {
            eventService.saveDeadEvent(event, e.toString());
            return null;
        }
        
    }
}
