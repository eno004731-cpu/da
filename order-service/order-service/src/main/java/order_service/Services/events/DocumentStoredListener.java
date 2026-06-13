package order_service.Services.events;

import order_service.Dto.payload.DocumentStoredPayload;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DocumentStoredListener {
    private final DocumentStoredEventService documentStoredEventService;

    @KafkaListener(
            topics = "${app.kafka.topics.document-stored}",
            containerFactory = "documentStoredKafkaListenerContainerFactory"
    )
    public void listenDocumentStored(ConsumerRecord<String, DocumentStoredPayload> record) {
        documentStoredEventService.handleDocumentStored(record);
    }
}
