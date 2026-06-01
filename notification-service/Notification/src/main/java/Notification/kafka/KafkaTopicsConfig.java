package Notification.kafka;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.apache.kafka.clients.admin.NewTopic;

@Configuration
public class KafkaTopicsConfig {
    @Bean
    NewTopic newTopic(){
        return TopicBuilder.name("auth.email-verification.requested")
        .partitions(5)
        .replicas(1)
        .build();
    }
     
    

}
