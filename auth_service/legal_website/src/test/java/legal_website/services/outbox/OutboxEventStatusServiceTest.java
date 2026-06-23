package legal_website.services.outbox;

import legal_website.persistence.outbox_events.OutboxEventEntity;
import legal_website.persistence.outbox_events.OutboxEventsRepo;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxEventStatusServiceTest {

    @Test
    void markProcessing_setsLeaseAndKeepsPreviousError() {
        TestContext context = context();
        LocalDateTime processingStartedAt =
                LocalDateTime.of(2026, 6, 23, 15, 0);
        OutboxEventEntity event = event("FAILED", 2);
        event.setLastError("Kafka unavailable");
        event.setLastErrorAt(processingStartedAt.minusSeconds(5));
        event.setNextRetryAt(processingStartedAt.minusSeconds(1));

        context.service.markProcessing(event, processingStartedAt);

        assertThat(event.getStatus()).isEqualTo("PROCESSING");
        assertThat(event.getProcessingStartedAt())
                .isEqualTo(processingStartedAt);
        assertThat(event.getProcessingToken()).isNotNull();
        assertThat(event.getNextRetryAt()).isNull();
        assertThat(event.getLastError()).isEqualTo("Kafka unavailable");
        assertThat(event.getLastErrorAt()).isNotNull();
        verify(context.repository).save(event);
    }

    @Test
    void markPublished_clearsLeaseButKeepsLastError() {
        TestContext context = context();
        OutboxEventEntity event = processingEvent();
        UUID processingToken = event.getProcessingToken();
        event.setLastError("Previous timeout");
        event.setLastErrorAt(LocalDateTime.now().minusMinutes(1));

        when(context.repository.findByIdAndStatusAndProcessingToken(
                event.getId(),
                "PROCESSING",
                processingToken
        )).thenReturn(Optional.of(event));

        context.service.markPublished(event.getId(), processingToken);

        assertThat(event.getStatus()).isEqualTo("PUBLISHED");
        assertThat(event.getPublishedAt()).isNotNull();
        assertThat(event.getProcessingStartedAt()).isNull();
        assertThat(event.getProcessingToken()).isNull();
        assertThat(event.getNextRetryAt()).isNull();
        assertThat(event.getLastError()).isEqualTo("Previous timeout");
        assertThat(event.getLastErrorAt()).isNotNull();
        verify(context.repository).save(event);
    }

    @Test
    void markFailed_beforeRetryLimit_setsFailedAndSchedulesRetry() {
        TestContext context = context();
        OutboxEventEntity event = processingEvent();
        event.setRetryCount(2);
        UUID processingToken = event.getProcessingToken();

        when(context.repository.findByIdAndStatusAndProcessingToken(
                event.getId(),
                "PROCESSING",
                processingToken
        )).thenReturn(Optional.of(event));

        LocalDateTime beforeCall = LocalDateTime.now();
        context.service.markFailed(
                event.getId(),
                processingToken,
                "Kafka unavailable"
        );

        assertThat(event.getStatus()).isEqualTo("FAILED");
        assertThat(event.getRetryCount()).isEqualTo(3);
        assertThat(event.getLastError()).isEqualTo("Kafka unavailable");
        assertThat(event.getLastErrorAt()).isNotNull();
        assertThat(event.getNextRetryAt())
                .isAfterOrEqualTo(beforeCall.plusSeconds(5))
                .isBeforeOrEqualTo(LocalDateTime.now().plusSeconds(5));
        assertThat(event.getProcessingStartedAt()).isNull();
        assertThat(event.getProcessingToken()).isNull();
        verify(context.repository).save(event);
    }

    @Test
    void markFailed_onFifthAttempt_setsDeadWithoutNextRetry() {
        TestContext context = context();
        OutboxEventEntity event = processingEvent();
        event.setRetryCount(4);
        UUID processingToken = event.getProcessingToken();

        when(context.repository.findByIdAndStatusAndProcessingToken(
                event.getId(),
                "PROCESSING",
                processingToken
        )).thenReturn(Optional.of(event));

        context.service.markFailed(
                event.getId(),
                processingToken,
                "Kafka unavailable"
        );

        assertThat(event.getStatus()).isEqualTo("DEAD");
        assertThat(event.getRetryCount()).isEqualTo(5);
        assertThat(event.getNextRetryAt()).isNull();
        assertThat(event.getProcessingStartedAt()).isNull();
        assertThat(event.getProcessingToken()).isNull();
        verify(context.repository).save(event);
    }

    @Test
    void callbackWithWrongToken_doesNotChangeCurrentAttempt() {
        TestContext context = context();
        OutboxEventEntity event = processingEvent();

        when(context.repository.findByIdAndStatusAndProcessingToken(
                event.getId(),
                "PROCESSING",
                UUID.fromString("00000000-0000-0000-0000-000000000099")
        )).thenReturn(Optional.empty());

        context.service.markPublished(
                event.getId(),
                UUID.fromString("00000000-0000-0000-0000-000000000099")
        );

        assertThat(event.getStatus()).isEqualTo("PROCESSING");
        verify(context.repository, never()).save(event);
    }

    @Test
    void markDead_setsPermanentFailureAndClearsLease() {
        TestContext context = context();
        OutboxEventEntity event = processingEvent();

        context.service.markDead(event, "User not found");

        assertThat(event.getStatus()).isEqualTo("DEAD");
        assertThat(event.getRetryCount()).isEqualTo(5);
        assertThat(event.getLastError()).isEqualTo("User not found");
        assertThat(event.getLastErrorAt()).isNotNull();
        assertThat(event.getNextRetryAt()).isNull();
        assertThat(event.getProcessingStartedAt()).isNull();
        assertThat(event.getProcessingToken()).isNull();
        verify(context.repository).save(event);
    }

    private TestContext context() {
        OutboxEventsRepo repository = mock(OutboxEventsRepo.class);
        return new TestContext(
                new OutboxEventStatusService(repository),
                repository
        );
    }

    private OutboxEventEntity processingEvent() {
        OutboxEventEntity event = event("PROCESSING", 0);
        event.setProcessingStartedAt(LocalDateTime.now().minusSeconds(1));
        event.setProcessingToken(UUID.randomUUID());
        return event;
    }

    private OutboxEventEntity event(String status, int retryCount) {
        OutboxEventEntity event = new OutboxEventEntity();
        event.setId(UUID.randomUUID());
        event.setEventType("DELETE_ALL_ORDERS");
        event.setStatus(status);
        event.setRetryCount(retryCount);
        return event;
    }

    private static class TestContext {
        private final OutboxEventStatusService service;
        private final OutboxEventsRepo repository;

        private TestContext(
                OutboxEventStatusService service,
                OutboxEventsRepo repository
        ) {
            this.service = service;
            this.repository = repository;
        }
    }
}
