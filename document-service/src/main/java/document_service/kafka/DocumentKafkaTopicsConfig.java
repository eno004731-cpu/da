package document_service.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

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
}
