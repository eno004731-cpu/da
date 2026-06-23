package document_service.configs;

import document_service.dto.payload.DocumentToDeletePayload;
import document_service.dto.payload.DocumentValidationResultPayload;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class DocumentKafkaTopicsConfig {
    public static final String DOCUMENT_UPLOAD_REQUESTED_TOPIC = "document.upload.requested";
    public static final String DOCUMENT_DELETE_REQUESTED_TOPIC = "document.delete-requested";
    public static final String DOCUMENT_STORED_TOPIC = "document.stored";
    public static final String DOCUMENT_DELETED_TOPIC = "document.deleted";
    public static final String DOCUMENT_VALIDATION_RESULT_TOPIC = "document.validation-result";

    @Bean
    public NewTopic documentUploadRequestedTopic() {
        return TopicBuilder.name(DOCUMENT_UPLOAD_REQUESTED_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic documentDeleteRequestedTopic(
            @Value("${app.kafka.topics.document-delete-requested}") String topic) {
        return TopicBuilder.name(topic)
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
    public NewTopic documentDeletedTopic() {
        return TopicBuilder.name(DOCUMENT_DELETED_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic documentValidationResultTopic(
            @Value("${app.kafka.topics.document-validation-result}") String topic) {
        return TopicBuilder.name(topic)
                .partitions(5)
                .replicas(1)
                .build();
    }

    @Bean
    public ProducerFactory<String, Object> documentEventProducerFactory(
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
    public KafkaTemplate<String, Object> documentEventKafkaTemplate(
            ProducerFactory<String, Object> documentEventProducerFactory) {
        return new KafkaTemplate<>(documentEventProducerFactory);
    }

    @Bean
    public ConsumerFactory<String, DocumentToDeletePayload> documentDeleteConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${spring.kafka.consumer.group-id}") String groupId) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        JsonDeserializer<DocumentToDeletePayload> valueDeserializer =
                new JsonDeserializer<>(DocumentToDeletePayload.class);
        // Producer не отправляет Java type headers, поэтому тип payload задаётся здесь явно.
        valueDeserializer.setUseTypeHeaders(false);

        return new DefaultKafkaConsumerFactory<>(
                properties,
                new StringDeserializer(),
                valueDeserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DocumentToDeletePayload>
    documentDeleteKafkaListenerContainerFactory(
            ConsumerFactory<String, DocumentToDeletePayload> documentDeleteConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, DocumentToDeletePayload> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        // Factory связывает @KafkaListener с типизированным consumer-ом команды удаления.
        factory.setConsumerFactory(documentDeleteConsumerFactory);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, DocumentValidationResultPayload>
    documentValidationResultConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${spring.kafka.consumer.group-id}") String groupId) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        JsonDeserializer<DocumentValidationResultPayload> valueDeserializer =
                new JsonDeserializer<>(DocumentValidationResultPayload.class);
        valueDeserializer.setUseTypeHeaders(false);

        return new DefaultKafkaConsumerFactory<>(
                properties,
                new StringDeserializer(),
                valueDeserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DocumentValidationResultPayload>
    documentValidationResultKafkaListenerContainerFactory(
            ConsumerFactory<String, DocumentValidationResultPayload>
                    documentValidationResultConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, DocumentValidationResultPayload> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(documentValidationResultConsumerFactory);
        return factory;
    }
}
