package order_service.integration;

import order_service.persistence.events.incoming.IncomingEventRepo;
import order_service.persistence.events.outbox.OutboxEventRepo;
import order_service.persistence.order.OrderRepo;
import order_service.services.catalog.ServiceNameOutboxService;
import order_service.services.events.handler.DocumentStoredEventService;
import order_service.services.events.outbox.EventStatusService;
import order_service.services.events.outbox.DocumentValidationOutboxService;
import order_service.services.orders.ClientOrderAccessService;
import order_service.services.orders.ClientOrderDetailsService;
import order_service.services.orders.ClientOrderDeleteService;
import order_service.services.orders.ClientOrdersQueryService;
import order_service.services.orders.ClientOrderUpdateService;
import order_service.services.orders.CreateOrderService;
import order_service.services.orders.OrderResponseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.show-sql=false",
        "spring.flyway.enabled=true",
        "spring.kafka.consumer.group-id=order-service-integration-test"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        IntegrationTestConfig.class,
        EventStatusService.class,
        ServiceNameOutboxService.class,
        ClientOrderAccessService.class,
        CreateOrderService.class,
        ClientOrderDetailsService.class,
        ClientOrdersQueryService.class,
        ClientOrderUpdateService.class,
        ClientOrderDeleteService.class,
        OrderResponseMapper.class,
        DocumentValidationOutboxService.class,
        DocumentStoredEventService.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
abstract class PostgresIntegrationTestBase {
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("legal_orders_test")
            .withUsername("legal_app")
            .withPassword("legal_pass");

    static {
        // Один контейнер на весь JVM-прогон, чтобы Spring context cache не держал устаревший JDBC URL.
        postgres.start();
    }

    @DynamicPropertySource
    static void registerPostgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private IncomingEventRepo incomingEventRepo;

    @Autowired
    private OutboxEventRepo outboxEventRepo;

    @Autowired
    private OrderRepo orderRepo;

    @BeforeEach
    void cleanDatabase() {
        // Удаляем inbox/outbox записи раньше агрегата заказа.
        incomingEventRepo.deleteAll();
        outboxEventRepo.deleteAll();
        orderRepo.deleteAll();
    }

}
