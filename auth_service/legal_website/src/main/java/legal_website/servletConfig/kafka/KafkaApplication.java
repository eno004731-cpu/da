package legal_website.servletConfig.kafka;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import legal_website.Dto.verityEmail.VerityEmailPayload;

@Configuration
public class KafkaApplication {
    @Bean
    public NewTopic verificationCodesEventsTopic() {
        return TopicBuilder.name("auth.email-verification.requested")
                .partitions(5)
                .replicas(1)
                .build();
    }
    
    @Bean
    public ProducerFactory<String, VerityEmailPayload> verificationEmailProducerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {

        // Делаем producer явным, чтобы Spring точно создал template
        // именно под наш payload, а не только общий Object/Object bean.
        Map<String, Object> properties = new HashMap<>();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                org.apache.kafka.common.serialization.StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        properties.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        return new DefaultKafkaProducerFactory<>(properties);
    }

    
    
    
}
