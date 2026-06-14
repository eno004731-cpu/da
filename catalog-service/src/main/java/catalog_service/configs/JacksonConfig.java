package catalog_service.configs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        // Catalog outbox/inbox код сериализует payload сам, поэтому ObjectMapper объявлен явно.
        // Это делает compose-запуск независимым от web auto-configuration.
        return new ObjectMapper().findAndRegisterModules();
    }
}
