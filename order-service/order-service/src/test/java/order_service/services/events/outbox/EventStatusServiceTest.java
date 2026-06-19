package order_service.services.events.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import order_service.persistence.events.incoming.IncomingEventEntity;
import order_service.persistence.events.incoming.IncomingEventRepo;
import order_service.persistence.events.outbox.OutboxEventEntity;
import order_service.persistence.events.outbox.OutboxEventRepo;

@ExtendWith(MockitoExtension.class)
class EventStatusServiceTest {

    @Mock
    private OutboxEventRepo outboxEventRepo;

    @Mock
    private IncomingEventRepo incomingEventRepo;

    @InjectMocks
    private EventStatusService service;

    @Test
    void savePublishedEvent_setsPublishedState() {
        OutboxEventEntity event = new OutboxEventEntity();
        event.setRetryCount(3);
        event.setLastError("old error");

        service.savePublishedEvent(event);

        assertEquals("PUBLISHED", event.getStatus());
        assertEquals(0, event.getRetryCount());
        assertNull(event.getLastError());
        assertNull(event.getNextRetryAt());
        assertNotNull(event.getPublishedAt());
        verify(outboxEventRepo).save(same(event));
    }

    @Test
    void saveFailedEvent_setsFailedAndSchedulesRetry() {
        OutboxEventEntity event = new OutboxEventEntity();
        event.setRetryCount(0);

        service.saveFailedEvent(event, "kafka error");

        assertEquals("FAILED", event.getStatus());
        assertEquals(1, event.getRetryCount());
        assertEquals("kafka error", event.getLastError());
        assertNotNull(event.getNextRetryAt());
        verify(outboxEventRepo).save(same(event));
    }

    @Test
    void saveFailedEvent_setsDeadWhenRetryLimitReached() {
        OutboxEventEntity event = new OutboxEventEntity();
        event.setRetryCount(4);

        service.saveFailedEvent(event, "too many retries");

        assertEquals("DEAD", event.getStatus());
        assertEquals(5, event.getRetryCount());
        assertEquals("too many retries", event.getLastError());
        assertNull(event.getNextRetryAt());
        verify(outboxEventRepo).save(same(event));
    }

    @Test
    void saveDeadEvent_setsDeadAndClearsNextRetry() {
        OutboxEventEntity event = new OutboxEventEntity();
        event.setRetryCount(1);

        service.saveDeadEvent(event, "bad payload");

        assertEquals("DEAD", event.getStatus());
        assertEquals(2, event.getRetryCount());
        assertEquals("bad payload", event.getLastError());
        assertNull(event.getNextRetryAt());
        verify(outboxEventRepo).save(same(event));
    }

    @Test
    void saveProcessedIncomingEvent_setsProcessedState() {
        IncomingEventEntity event = new IncomingEventEntity();
        event.setLastError("old error");
        event.setNextRetryAt(java.time.LocalDateTime.now());

        service.saveProcessedIncomingEvent(event);

        assertEquals("PROCESSED", event.getStatus());
        assertNotNull(event.getProcessedAt());
        assertNull(event.getLastError());
        assertNull(event.getNextRetryAt());
        verify(incomingEventRepo).save(same(event));
    }

    @Test
    void saveDeadIncomingEvent_setsDeadState() {
        IncomingEventEntity event = new IncomingEventEntity();
        event.setRetryCount(2);

        service.saveDeadIncomingEvent(event, "catalog response is invalid");

        assertEquals("DEAD", event.getStatus());
        assertEquals(3, event.getRetryCount());
        assertEquals("catalog response is invalid", event.getLastError());
        assertNull(event.getNextRetryAt());
        assertNotNull(event.getProcessedAt());
        verify(incomingEventRepo).save(same(event));
    }
}
