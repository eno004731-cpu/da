package order_service.services.events.consumer;

import lombok.RequiredArgsConstructor;
import order_service.dto.payload.DocumentDeletedPayload;
import order_service.services.events.handler.DocumentDeletedEventService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DocumentDeletedListener {
    private final DocumentDeletedEventService documentDeletedEventService;

    @KafkaListener(
            topics = "${app.kafka.topics.document-deleted}",
            containerFactory = "documentDeletedKafkaListenerContainerFactory"
    )
    public void listenDocumentDeleted(ConsumerRecord<String, DocumentDeletedPayload> record) {
        documentDeletedEventService.handleDocumentDeleted(record);
    }
}
