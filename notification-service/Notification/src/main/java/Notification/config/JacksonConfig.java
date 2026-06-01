package Notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        // Явно регистрируем bean, чтобы сервис отправки писем
        // мог безопасно сериализовать и читать payload уведомлений.
        return new ObjectMapper();
    }
}
