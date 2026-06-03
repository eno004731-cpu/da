package Notification.kafka;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import Notification.Dto.VerityEmailPayload;

@Configuration
public class KafkaTopicsConfig {
    @Bean
    NewTopic newTopic(){
        return TopicBuilder.name("auth.email-verification.requested")
        .partitions(5)
        .replicas(1)
        .build();
    }

    @Bean
    public ConsumerFactory<String, VerityEmailPayload> verificationEmailConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-service");
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        properties.put(JsonDeserializer.TRUSTED_PACKAGES, "Notification.Dto");
        properties.put(JsonDeserializer.VALUE_DEFAULT_TYPE, VerityEmailPayload.class.getName());
        properties.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        return new DefaultKafkaConsumerFactory<>(properties);
    }

    @Bean(name = "kafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, VerityEmailPayload> kafkaListenerContainerFactory(
            ConsumerFactory<String, VerityEmailPayload> verificationEmailConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, VerityEmailPayload> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(verificationEmailConsumerFactory);
        return factory;
    }
}
