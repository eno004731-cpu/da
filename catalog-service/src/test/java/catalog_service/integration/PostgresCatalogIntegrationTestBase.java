package catalog_service.integration;

import catalog_service.catalog.ServiceCatalogService;
import catalog_service.entityAndRepo.inbox.InboxEventRepo;
import catalog_service.entityAndRepo.outbox.OutboxEventRepo;
import catalog_service.services.CreateOutboxEvent;
import catalog_service.services.EventService;
import catalog_service.services.ListenKafkaService;
import org.junit.jupiter.api.BeforeEach;
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
        IntegrationTestConfig.class,
        ServiceCatalogService.class,
        EventService.class,
        ListenKafkaService.class,
        CreateOutboxEvent.class
})
abstract class PostgresCatalogIntegrationTestBase {

    // Singleton container ускоряет набор тестов и не даёт Spring context cache поймать старый JDBC URL.
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("catalog_test")
                    .withUsername("catalog_test")
                    .withPassword("catalog_test");

    static {
        POSTGRES.start();
    }

    @Autowired
    InboxEventRepo inboxEventRepo;

    @Autowired
    OutboxEventRepo outboxEventRepo;

    @DynamicPropertySource
    static void registerPostgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.show-sql", () -> "false");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @BeforeEach
    void cleanEventTables() {
        // Seed services оставляем: они приходят из Flyway и нужны как справочник.
        outboxEventRepo.deleteAll();
        inboxEventRepo.deleteAll();
    }
}
