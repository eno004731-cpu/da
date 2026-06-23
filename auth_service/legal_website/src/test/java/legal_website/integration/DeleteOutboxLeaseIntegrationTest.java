package legal_website.integration;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import legal_website.persistence.outbox_events.OutboxEventEntity;
import legal_website.services.delete.DeleteOutboxClaimService;
import legal_website.services.delete.DeleteOutboxEventType;
import legal_website.services.outbox.OutboxEventStatusService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Import({
        OutboxEventStatusService.class,
        DeleteOutboxClaimService.class
})
class DeleteOutboxLeaseIntegrationTest
        extends PostgresAuthIntegrationTestBase {

    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 6, 23, 15, 0);

    @Autowired
    private DeleteOutboxClaimService claimService;

    @Autowired
    private OutboxEventStatusService statusService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void claim_selectsNewDeletionEventsAndIgnoresEmailEvent() {
        OutboxEventEntity orderEvent = outboxEventsRepo.save(
                event(
                        DeleteOutboxEventType.DELETE_ALL_ORDERS.name(),
                        "NEW",
                        NOW.minusMinutes(2)
                )
        );
        OutboxEventEntity documentEvent = outboxEventsRepo.save(
                event(
                        DeleteOutboxEventType.DELETE_ALL_DOCUMENTS.name(),
                        "NEW",
                        NOW.minusMinutes(1)
                )
        );
        OutboxEventEntity emailEvent = outboxEventsRepo.save(
                event(
                        "EMAIL_VERIFICATION_REQUESTED",
                        "NEW",
                        NOW
                )
        );
        outboxEventsRepo.flush();

        List<OutboxEventEntity> claimed = claimService.claimAvailableEvents(
                NOW,
                NOW.minusMinutes(3)
        );
        outboxEventsRepo.flush();

        assertThat(claimed)
                .extracting(OutboxEventEntity::getId)
                .containsExactly(orderEvent.getId(), documentEvent.getId());
        assertThat(claimed)
                .allMatch(event -> "PROCESSING".equals(event.getStatus()));
        assertThat(claimed)
                .allMatch(event -> event.getProcessingToken() != null);

        OutboxEventEntity untouchedEmail =
                outboxEventsRepo.findById(emailEvent.getId()).orElseThrow();
        assertThat(untouchedEmail.getStatus()).isEqualTo("NEW");
        assertThat(untouchedEmail.getProcessingToken()).isNull();
    }

    @Test
    void claim_doesNotSelectFailedBeforeRetryOrProcessingBeforeTimeout() {
        OutboxEventEntity waitingForRetry = event(
                DeleteOutboxEventType.DELETE_ALL_ORDERS.name(),
                "FAILED",
                NOW.minusMinutes(2)
        );
        waitingForRetry.setNextRetryAt(NOW.plusSeconds(1));
        waitingForRetry = outboxEventsRepo.save(waitingForRetry);

        OutboxEventEntity activeProcessing = event(
                DeleteOutboxEventType.DELETE_ALL_DOCUMENTS.name(),
                "PROCESSING",
                NOW.minusMinutes(2)
        );
        activeProcessing.setProcessingStartedAt(NOW.minusMinutes(2));
        activeProcessing.setProcessingToken(UUID.randomUUID());
        activeProcessing = outboxEventsRepo.save(activeProcessing);
        outboxEventsRepo.flush();

        List<OutboxEventEntity> claimed = claimService.claimAvailableEvents(
                NOW,
                NOW.minusMinutes(3)
        );

        assertThat(claimed).isEmpty();
        assertThat(
                outboxEventsRepo.findById(waitingForRetry.getId())
                        .orElseThrow()
                        .getStatus()
        ).isEqualTo("FAILED");
        assertThat(
                outboxEventsRepo.findById(activeProcessing.getId())
                        .orElseThrow()
                        .getStatus()
        ).isEqualTo("PROCESSING");
    }

    @Test
    void claim_selectsFailedEventAfterRetryTimeReached() {
        OutboxEventEntity retryReady = event(
                DeleteOutboxEventType.DELETE_ALL_ORDERS.name(),
                "FAILED",
                NOW.minusMinutes(2)
        );
        retryReady.setRetryCount(1);
        retryReady.setNextRetryAt(NOW.minusSeconds(1));
        retryReady = outboxEventsRepo.save(retryReady);
        outboxEventsRepo.flush();

        List<OutboxEventEntity> claimed = claimService.claimAvailableEvents(
                NOW,
                NOW.minusMinutes(3)
        );

        assertThat(claimed)
                .extracting(OutboxEventEntity::getId)
                .containsExactly(retryReady.getId());
        assertThat(claimed.get(0).getStatus()).isEqualTo("PROCESSING");
        assertThat(claimed.get(0).getRetryCount()).isEqualTo(1);
        assertThat(claimed.get(0).getNextRetryAt()).isNull();
        assertThat(claimed.get(0).getProcessingToken()).isNotNull();
    }

    @Test
    void claim_recoversTimedOutProcessingEvent() {
        UUID oldProcessingToken = UUID.randomUUID();
        OutboxEventEntity timedOutEvent = event(
                DeleteOutboxEventType.DELETE_ALL_DOCUMENTS.name(),
                "PROCESSING",
                NOW.minusMinutes(10)
        );
        timedOutEvent.setRetryCount(1);
        timedOutEvent.setProcessingStartedAt(NOW.minusMinutes(4));
        timedOutEvent.setProcessingToken(oldProcessingToken);
        timedOutEvent = outboxEventsRepo.save(timedOutEvent);
        outboxEventsRepo.flush();

        List<OutboxEventEntity> claimed = claimService.claimAvailableEvents(
                NOW,
                NOW.minusMinutes(3)
        );
        outboxEventsRepo.flush();

        assertThat(claimed)
                .extracting(OutboxEventEntity::getId)
                .containsExactly(timedOutEvent.getId());

        OutboxEventEntity recovered =
                outboxEventsRepo.findById(timedOutEvent.getId()).orElseThrow();
        assertThat(recovered.getStatus()).isEqualTo("PROCESSING");
        assertThat(recovered.getRetryCount()).isEqualTo(2);
        assertThat(recovered.getLastError())
                .isEqualTo("Processing timeout: callback was not completed");
        assertThat(recovered.getLastErrorAt()).isNotNull();
        assertThat(recovered.getProcessingStartedAt()).isEqualTo(NOW);
        assertThat(recovered.getProcessingToken())
                .isNotEqualTo(oldProcessingToken);
    }

    @Test
    void callback_withOldTokenDoesNotChangeCurrentAttempt() {
        UUID currentToken = UUID.randomUUID();
        OutboxEventEntity event = event(
                DeleteOutboxEventType.DELETE_ALL_ORDERS.name(),
                "PROCESSING",
                NOW.minusMinutes(1)
        );
        event.setProcessingStartedAt(NOW);
        event.setProcessingToken(currentToken);
        event.setLastError("Previous timeout");
        event.setLastErrorAt(NOW.minusSeconds(5));
        event = outboxEventsRepo.save(event);
        outboxEventsRepo.flush();

        statusService.markPublished(event.getId(), UUID.randomUUID());

        OutboxEventEntity afterOldCallback =
                outboxEventsRepo.findById(event.getId()).orElseThrow();
        assertThat(afterOldCallback.getStatus()).isEqualTo("PROCESSING");
        assertThat(afterOldCallback.getProcessingToken())
                .isEqualTo(currentToken);

        statusService.markPublished(event.getId(), currentToken);

        OutboxEventEntity published =
                outboxEventsRepo.findById(event.getId()).orElseThrow();
        assertThat(published.getStatus()).isEqualTo("PUBLISHED");
        assertThat(published.getProcessingStartedAt()).isNull();
        assertThat(published.getProcessingToken()).isNull();
        assertThat(published.getLastError()).isEqualTo("Previous timeout");
        assertThat(published.getLastErrorAt()).isNotNull();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void selectForUpdateSkipLocked_allowsOnlyOneReplicaToClaimEvent()
            throws Exception {
        OutboxEventEntity event = outboxEventsRepo.save(
                event(
                        DeleteOutboxEventType.DELETE_ALL_ORDERS.name(),
                        "NEW",
                        NOW.minusMinutes(1)
                )
        );

        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);
        CountDownLatch firstReplicaLockedRow = new CountDownLatch(1);
        CountDownLatch allowFirstReplicaToFinish = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<List<OutboxEventEntity>> firstReplica =
                    executor.submit(() -> transaction.execute(status -> {
                        List<OutboxEventEntity> locked =
                                outboxEventsRepo.findDeletionEventsForUpdate(
                                        List.of(
                                                DeleteOutboxEventType
                                                        .DELETE_ALL_ORDERS
                                                        .name()
                                        ),
                                        NOW,
                                        NOW.minusMinutes(3)
                                );
                        firstReplicaLockedRow.countDown();
                        await(allowFirstReplicaToFinish);
                        return locked;
                    }));

            assertThat(
                    firstReplicaLockedRow.await(5, TimeUnit.SECONDS)
            ).isTrue();

            Future<List<OutboxEventEntity>> secondReplica =
                    executor.submit(() -> transaction.execute(status ->
                            outboxEventsRepo.findDeletionEventsForUpdate(
                                    List.of(
                                            DeleteOutboxEventType
                                                    .DELETE_ALL_ORDERS
                                                    .name()
                                    ),
                                    NOW,
                                    NOW.minusMinutes(3)
                            )
                    ));

            assertThat(secondReplica.get(5, TimeUnit.SECONDS)).isEmpty();

            allowFirstReplicaToFinish.countDown();
            assertThat(firstReplica.get(5, TimeUnit.SECONDS))
                    .extracting(OutboxEventEntity::getId)
                    .containsExactly(event.getId());
        } finally {
            allowFirstReplicaToFinish.countDown();
            executor.shutdownNow();
            outboxEventsRepo.deleteById(event.getId());
        }
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                        "Timed out while waiting for concurrent test"
                );
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private OutboxEventEntity event(
            String eventType,
            String status,
            LocalDateTime createdAt
    ) {
        OutboxEventEntity event = new OutboxEventEntity();
        event.setAggregateType("USER");
        event.setAggregateId("17");
        event.setEventType(eventType);
        event.setPayload(JsonNodeFactory.instance.objectNode().put("id", 17L));
        event.setStatus(status);
        event.setRetryCount(0);
        event.setCreatedAt(createdAt);
        return event;
    }
}
