package document_service.integration;

import document_service.persistence.document.DocumentRepository;
import document_service.persistence.events.outbox.OutboxEventRepository;
import document_service.persistence.events.incoming.ProcessedEventRepository;
import document_service.services.documents.DocumentResponseMapper;
import document_service.services.documents.store.DocumentFileStorage;
import document_service.services.documents.store.OrderDocumentsService;
import document_service.services.events.DocumentOutboxEventFactory;
import document_service.services.events.DocumentStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileSystemUtils;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.show-sql=false",
        "spring.flyway.enabled=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        IntegrationTestConfig.class,
        DocumentOutboxEventFactory.class,
        DocumentStatusService.class,
        DocumentFileStorage.class,
        DocumentResponseMapper.class,
        OrderDocumentsService.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
abstract class PostgresDocumentIntegrationTestBase {
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("legal_documents_test")
            .withUsername("legal_app")
            .withPassword("legal_pass");

    static final Path documentsDir = createTempDocumentsDir();

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.storage.documents-dir", () -> documentsDir.toString());
    }

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @BeforeEach
    void cleanState() {
        processedEventRepository.deleteAll();
        outboxEventRepository.deleteAll();
        documentRepository.deleteAll();
        FileSystemUtils.deleteRecursively(documentsDir.toFile());
        try {
            Files.createDirectories(documentsDir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Path createTempDocumentsDir() {
        try {
            return Files.createTempDirectory("document-service-integration-");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
