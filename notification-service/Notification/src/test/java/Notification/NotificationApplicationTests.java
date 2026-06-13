package Notification;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.mockito.Mockito.mock;

@SpringBootTest
class NotificationApplicationTests {

	private static final PostgreSQLContainer<?> POSTGRES =
			new PostgreSQLContainer<>("postgres:16-alpine")
					.withDatabaseName("notification_context_test")
					.withUsername("notification_context_test")
					.withPassword("notification_context_test");

	static {
		POSTGRES.start();
	}

	@DynamicPropertySource
	static void registerPostgresProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
		registry.add("spring.flyway.enabled", () -> "true");
		registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");
		registry.add("spring.kafka.listener.auto-startup", () -> "false");
		registry.add("spring.kafka.admin.auto-create", () -> "false");
		registry.add("app.mail.from", () -> "noreply@test.local");
		registry.add("app.notification-worker.fixed-delay-ms", () -> "600000");
	}

	@Test
	void contextLoads() {
	}

	@TestConfiguration
	static class NotificationApplicationTestConfig {

		@Bean
		JavaMailSender javaMailSender() {
			return new JavaMailSenderImpl() {
				@Override
				public void send(MimeMessage mimeMessage) {
					// Smoke test не должен отправлять реальные письма.
				}

				@Override
				public void send(MimeMessage... mimeMessages) {
					// Batch-send тоже оставляем no-op.
				}
			};
		}

		@Bean
		KafkaTemplate<?, ?> kafkaTemplate() {
			// Нужен только для создания production DefaultErrorHandler в smoke test.
			return mock(KafkaTemplate.class);
		}
	}
}
