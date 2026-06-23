package legal_website.services.delete;

import legal_website.persistence.outbox_events.OutboxEventEntity;
import legal_website.persistence.outbox_events.OutboxEventsRepo;
import legal_website.services.outbox.OutboxEventStatusService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeleteOutboxClaimServiceTest {

    @Test
    void claimAvailableEvents_marksNewEventAsProcessing() {
        TestContext context = context();
        LocalDateTime now = LocalDateTime.of(2026, 6, 23, 15, 0);
        OutboxEventEntity event = event("NEW", 0);
        when(context.repository.findDeletionEventsForUpdate(
                anyList(),
                org.mockito.ArgumentMatchers.eq(now),
                org.mockito.ArgumentMatchers.eq(now.minusMinutes(3))
        )).thenReturn(List.of(event));

        List<OutboxEventEntity> claimed =
                context.service.claimAvailableEvents(
                        now,
                        now.minusMinutes(3)
                );

        assertThat(claimed).containsExactly(event);
        verify(context.statusService).markProcessing(event, now);
        verify(context.statusService, never())
                .markFailed(
                        org.mockito.ArgumentMatchers.any(OutboxEventEntity.class),
                        org.mockito.ArgumentMatchers.anyString()
                );
    }

    @Test
    void claimAvailableEvents_recoversTimedOutProcessingEvent() {
        TestContext context = context();
        LocalDateTime now = LocalDateTime.of(2026, 6, 23, 15, 0);
        OutboxEventEntity event = event("PROCESSING", 1);
        when(context.repository.findDeletionEventsForUpdate(
                anyList(),
                org.mockito.ArgumentMatchers.eq(now),
                org.mockito.ArgumentMatchers.eq(now.minusMinutes(3))
        )).thenReturn(List.of(event));
        org.mockito.Mockito.doAnswer(invocation -> {
            event.setStatus("FAILED");
            event.setRetryCount(2);
            return null;
        }).when(context.statusService).markFailed(
                event,
                "Processing timeout: callback was not completed"
        );

        List<OutboxEventEntity> claimed =
                context.service.claimAvailableEvents(
                        now,
                        now.minusMinutes(3)
                );

        assertThat(claimed).containsExactly(event);
        verify(context.statusService).markFailed(
                event,
                "Processing timeout: callback was not completed"
        );
        verify(context.statusService).markProcessing(event, now);
    }

    @Test
    void claimAvailableEvents_doesNotClaimTimedOutEventAfterFifthFailure() {
        TestContext context = context();
        LocalDateTime now = LocalDateTime.of(2026, 6, 23, 15, 0);
        OutboxEventEntity event = event("PROCESSING", 4);
        when(context.repository.findDeletionEventsForUpdate(
                anyList(),
                org.mockito.ArgumentMatchers.eq(now),
                org.mockito.ArgumentMatchers.eq(now.minusMinutes(3))
        )).thenReturn(List.of(event));
        org.mockito.Mockito.doAnswer(invocation -> {
            event.setStatus("DEAD");
            event.setRetryCount(5);
            return null;
        }).when(context.statusService).markFailed(
                event,
                "Processing timeout: callback was not completed"
        );

        List<OutboxEventEntity> claimed =
                context.service.claimAvailableEvents(
                        now,
                        now.minusMinutes(3)
                );

        assertThat(claimed).isEmpty();
        verify(context.statusService, never()).markProcessing(event, now);
    }

    private TestContext context() {
        OutboxEventsRepo repository = mock(OutboxEventsRepo.class);
        OutboxEventStatusService statusService =
                mock(OutboxEventStatusService.class);
        return new TestContext(
                new DeleteOutboxClaimService(repository, statusService),
                repository,
                statusService
        );
    }

    private OutboxEventEntity event(String status, int retryCount) {
        OutboxEventEntity event = new OutboxEventEntity();
        event.setStatus(status);
        event.setRetryCount(retryCount);
        event.setEventType(DeleteOutboxEventType.DELETE_ALL_ORDERS.name());
        return event;
    }

    private static class TestContext {
        private final DeleteOutboxClaimService service;
        private final OutboxEventsRepo repository;
        private final OutboxEventStatusService statusService;

        private TestContext(
                DeleteOutboxClaimService service,
                OutboxEventsRepo repository,
                OutboxEventStatusService statusService
        ) {
            this.service = service;
            this.repository = repository;
            this.statusService = statusService;
        }
    }
}
