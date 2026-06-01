package legal_website.servletConfig;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        // Регистрируем стандартные jackson-модули, чтобы mapper умел
        // сериализовать LocalDateTime и другие java.time-типы.
        return new ObjectMapper().findAndRegisterModules();
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
