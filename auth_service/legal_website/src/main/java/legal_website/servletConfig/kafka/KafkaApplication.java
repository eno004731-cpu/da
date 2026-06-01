package legal_website.servletConfig.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaApplication {
    public static void main(String[] args) {
        // Запуск Spring Boot приложения
    }
    @Bean
    public NewTopic verificationCodesEventsTopic() {
        return TopicBuilder.name("auth.email-verification.requested")
                .partitions(5)
                .replicas(1)
                .build();
    }
}
