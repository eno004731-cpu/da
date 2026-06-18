package document_service.configs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        // Outbox payload сериализуется вручную, поэтому сервису нужен явный ObjectMapper bean.
        // findAndRegisterModules добавляет поддержку Java time типов вроде Instant/LocalDateTime.
        return new ObjectMapper().findAndRegisterModules();
    }
}
