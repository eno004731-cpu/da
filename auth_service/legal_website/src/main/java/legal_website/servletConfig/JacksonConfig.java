package legal_website.servletConfig;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        // Регистрируем стандартные jackson-модули, чтобы mapper умел
        // сериализовать LocalDateTime и другие java.time-типы.
        return new ObjectMapper().findAndRegisterModules();
    }
}
