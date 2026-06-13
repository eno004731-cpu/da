package Notification.integration;

import Notification.EntityAndRepo.Events.EventRepo;
import Notification.EntityAndRepo.Nofilication.NofilicationRepo;
import Notification.Services.SendEmail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        NotificationIntegrationTestConfig.class,
        SendEmail.class
})
abstract class PostgresNotificationIntegrationTestBase {

    // Singleton container ускоряет тесты и держит один стабильный JDBC URL для Spring context cache.
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("notification_test")
                    .withUsername("notification_test")
                    .withPassword("notification_test");

    static {
        POSTGRES.start();
    }

    @Autowired
    EventRepo eventRepo;

    @Autowired
    NofilicationRepo nofilicationRepo;

    @DynamicPropertySource
    static void registerPostgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.show-sql", () -> "false");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("app.mail.from", () -> "noreply@test.local");
        registry.add("app.notification-worker.processing-timeout-seconds", () -> "120");
    }
}
