package legal_website.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@TestConfiguration
class AuthIntegrationTestConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        // Используем тот же тип encoder'а, что и production SecurityConfig.
        return new BCryptPasswordEncoder();
    }

    @Bean
    ObjectMapper objectMapper() {
        // Outbox хранит JSONB, поэтому в тесте нужен такой же mapper с поддержкой java.time.
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
