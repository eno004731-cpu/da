package catalog_service.integration;

import catalog_service.dto.payload.GetServiceNamePayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.Map;

@TestConfiguration
class IntegrationTestConfig {

    @Bean
    ObjectMapper objectMapper() {
        // В slice-тесте поднимается не весь Boot context, поэтому ObjectMapper даём явно.
        return new ObjectMapper();
    }

    @Bean(name = "kafkaListenerContainerFactory")
    ConcurrentKafkaListenerContainerFactory<String, GetServiceNamePayload> kafkaListenerContainerFactory() {
        Map<String, Object> properties = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092",
                ConsumerConfig.GROUP_ID_CONFIG, "catalog-integration-test",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class,
                JsonDeserializer.VALUE_DEFAULT_TYPE, GetServiceNamePayload.class.getName(),
                JsonDeserializer.TRUSTED_PACKAGES, "catalog_service.dto.payload"
        );

        ConcurrentKafkaListenerContainerFactory<String, GetServiceNamePayload> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(properties));
        // Брокер в этих тестах не нужен: мы напрямую вызываем saveEvent(record).
        factory.setAutoStartup(false);
        return factory;
    }
}
