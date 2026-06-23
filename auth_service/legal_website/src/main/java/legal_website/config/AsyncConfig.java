package legal_website.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(3);      // сколько потоков держать постоянно
        executor.setMaxPoolSize(5);       // максимум потоков
        executor.setQueueCapacity(100);   // очередь задач

        executor.setThreadNamePrefix("async-");

        executor.setRejectedExecutionHandler(
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        executor.initialize();
        return executor;
    }

    /**
     * Отдельный пул не позволяет callback'ам удаления занять общий async executor.
     */
    @Bean(name = "deleteOutboxCallbackExecutor")
    public ThreadPoolTaskExecutor deleteOutboxCallbackExecutor(
            @Value("${app.outbox.delete.callback-executor.core-pool-size:2}")
            int corePoolSize,
            @Value("${app.outbox.delete.callback-executor.max-pool-size:4}")
            int maxPoolSize,
            @Value("${app.outbox.delete.callback-executor.queue-capacity:100}")
            int queueCapacity
    ) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("delete-outbox-callback-");
        executor.setRejectedExecutionHandler(
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        executor.initialize();
        return executor;
    }
}
