package legal_website.servletConfig;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        // Собираем mapper через Spring builder, чтобы не потерять
        // boot-автоконфигурацию и явно подключить java.time модуль.
        return builder
                .modules(new JavaTimeModule())
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
    }
    
    @Configuration
    @EnableScheduling
    public static class SchedulingConfig {

        @Bean
        public ThreadPoolTaskScheduler taskScheduler() {
            ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
            // Небольшой пул вместо single-thread scheduler по умолчанию.
            scheduler.setPoolSize(Math.max(2, Runtime.getRuntime().availableProcessors()));
            scheduler.setThreadNamePrefix("scheduled-task-");
            scheduler.setWaitForTasksToCompleteOnShutdown(true);
            scheduler.setAwaitTerminationSeconds(30);
            return scheduler;
        }
    }
}
