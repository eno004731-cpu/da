package order_service.configs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        // Order-service сам собирает JSON payload для outbox/read-model.
        // Явный bean убирает зависимость от web auto-configuration.
        return new ObjectMapper().findAndRegisterModules();
    }
}
