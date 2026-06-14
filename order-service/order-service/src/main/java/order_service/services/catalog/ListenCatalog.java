package order_service.services.catalog;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import order_service.dto.payload.GetServiceNamePayload;

@Component
@RequiredArgsConstructor
public class ListenCatalog {
    private final ListenCatalogService listenCatalogService;

    // Listener остаётся тонким адаптером: принял сообщение и передал в service.
    @KafkaListener(
            topics = "${app.kafka.topics.catalog-get-service-name-response}",
            containerFactory = "catalogResponseKafkaListenerContainerFactory"
    )
    public void listenCatalogResponse(ConsumerRecord<String, GetServiceNamePayload> record) {
        listenCatalogService.handleCatalogResponse(record);
    }
}
