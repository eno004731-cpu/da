package order_service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import order_service.services.documents.DocumentGateway;

import static org.mockito.Mockito.mock;

@TestConfiguration
class IntegrationTestConfig {
    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    DocumentGateway documentGateway() {
        return mock(DocumentGateway.class);
    }
}
