package document_service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
class IntegrationTestConfig {
    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
