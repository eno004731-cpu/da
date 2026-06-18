package order_service.configs;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
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
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import order_service.dto.payload.GetServiceNamePayload;
import order_service.dto.payload.DocumentStoredPayload;
import order_service.dto.payload.DocumentDeletedPayload;

@Configuration
public class KafkaCatalogConfig {

    @Bean
    public NewTopic getServiceNameRequestTopic(
            @Value("${app.kafka.topics.catalog-get-service-name-request}") String requestTopic
    ) {
        return TopicBuilder.name(requestTopic)
                .partitions(5)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic getServiceNameResponseTopic(
            @Value("${app.kafka.topics.catalog-get-service-name-response}") String responseTopic
    ) {
        return TopicBuilder.name(responseTopic)
                .partitions(5)
                .replicas(1)           .build();
    }

    @Bean
    public NewTopic documentStoredTopic(
            @Value("${app.kafka.topics.document-stored}") String documentStoredTopic
    ) {
        return TopicBuilder.name(documentStoredTopic)
                .partitions(5)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic documentDeletedTopic(
            @Value("${app.kafka.topics.document-deleted}") String documentDeletedTopic
    ) {
        return TopicBuilder.name(documentDeletedTopic)
                .partitions(5)
                .replicas(1)
                .build();
    }

    @Bean
    public ProducerFactory<String, GetServiceNamePayload> producerFactory(
            @Value("${SPRING_KAFKA_BOOTSTRAP_SERVERS:127.0.0.1:9092}") String bootstrapServers
    ) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(props);
    }
    @Bean
    public KafkaTemplate<String,GetServiceNamePayload> kafkaTemplateCatalog(ProducerFactory<String, GetServiceNamePayload> producerFactory){
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public ConsumerFactory<String, GetServiceNamePayload> catalogResponseConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${spring.kafka.consumer.group-id}") String groupId
    ) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        properties.put(JsonDeserializer.TRUSTED_PACKAGES, "order_service.dto.payload");
        properties.put(JsonDeserializer.VALUE_DEFAULT_TYPE, GetServiceNamePayload.class.getName());
        properties.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        return new DefaultKafkaConsumerFactory<>(properties);
    }

    @Bean(name = "catalogResponseKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, GetServiceNamePayload> catalogResponseKafkaListenerContainerFactory(
            ConsumerFactory<String, GetServiceNamePayload> catalogResponseConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, GetServiceNamePayload> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(catalogResponseConsumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, DocumentStoredPayload> documentStoredConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${spring.kafka.consumer.group-id}") String groupId
    ) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        properties.put(JsonDeserializer.TRUSTED_PACKAGES, "order_service.dto.payload");
        properties.put(JsonDeserializer.VALUE_DEFAULT_TYPE, DocumentStoredPayload.class.getName());
        properties.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        return new DefaultKafkaConsumerFactory<>(properties);
    }

    @Bean(name = "documentStoredKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, DocumentStoredPayload> documentStoredKafkaListenerContainerFactory(
            ConsumerFactory<String, DocumentStoredPayload> documentStoredConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, DocumentStoredPayload> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(documentStoredConsumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, DocumentDeletedPayload> documentDeletedConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${spring.kafka.consumer.group-id}") String groupId
    ) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        properties.put(JsonDeserializer.TRUSTED_PACKAGES, "order_service.dto.payload");
        properties.put(JsonDeserializer.VALUE_DEFAULT_TYPE, DocumentDeletedPayload.class.getName());
        properties.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        return new DefaultKafkaConsumerFactory<>(properties);
    }

    @Bean(name = "documentDeletedKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, DocumentDeletedPayload> documentDeletedKafkaListenerContainerFactory(
            ConsumerFactory<String, DocumentDeletedPayload> documentDeletedConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, DocumentDeletedPayload> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(documentDeletedConsumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        return factory;
    }
}
