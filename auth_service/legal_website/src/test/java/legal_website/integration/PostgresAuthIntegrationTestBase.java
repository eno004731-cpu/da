package legal_website.integration;

import legal_website.persistence.auth.UserRepo;
import legal_website.persistence.jwt.JwtRepo;
import legal_website.persistence.outbox_events.OutboxEventsRepo;
import legal_website.persistence.verification_codes.VerificationCodesRepo;
import legal_website.services.jwt.JwtService;
import legal_website.services.auth.AuthSessionService;
import legal_website.services.auth.register.RegService;
import legal_website.services.verifyemail.VerifyEmailService;
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
        AuthIntegrationTestConfig.class,
        JwtService.class,
        AuthSessionService.class,
        RegService.class,
        VerifyEmailService.class
})
abstract class PostgresAuthIntegrationTestBase {

    // Один контейнер на весь набор auth integration-тестов быстрее и стабильнее для Spring context cache.
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("auth_test")
                    .withUsername("auth_test")
                    .withPassword("auth_test");

    static {
        POSTGRES.start();
    }

    @Autowired
    UserRepo userRepo;

    @Autowired
    JwtRepo jwtRepo;

    @Autowired
    VerificationCodesRepo verificationCodesRepo;

    @Autowired
    OutboxEventsRepo outboxEventsRepo;

    @DynamicPropertySource
    static void registerPostgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.show-sql", () -> "false");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("jwt.secret", () -> "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        registry.add("jwt.access-minutes", () -> "15");
        registry.add("jwt.refresh-days", () -> "30");
        registry.add("app.frontend-base-url", () -> "http://127.0.0.1:8000");
    }
}
