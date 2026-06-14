package document_service.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import document_service.events.DocumentStoredPayload;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class DocumentKafkaTopicsConfig {
    public static final String DOCUMENT_UPLOAD_REQUESTED_TOPIC = "document.upload.requested";
    public static final String DOCUMENT_DELETE_REQUESTED_TOPIC = "document.delete.requested";
    public static final String DOCUMENT_STORED_TOPIC = "document.stored";

    @Bean
    public NewTopic documentUploadRequestedTopic() {
        return TopicBuilder.name(DOCUMENT_UPLOAD_REQUESTED_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic documentDeleteRequestedTopic() {
        return TopicBuilder.name(DOCUMENT_DELETE_REQUESTED_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic documentStoredTopic() {
        return TopicBuilder.name(DOCUMENT_STORED_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public ProducerFactory<String, DocumentStoredPayload> documentStoredProducerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        // Type headers отключены, чтобы consumer contract определялся topic + payload class.
        properties.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(properties);
    }

    @Bean
    public KafkaTemplate<String, DocumentStoredPayload> documentStoredKafkaTemplate(
            ProducerFactory<String, DocumentStoredPayload> documentStoredProducerFactory) {
        // Outbox publisher требует typed template для document.stored событий.
        return new KafkaTemplate<>(documentStoredProducerFactory);
    }
}
