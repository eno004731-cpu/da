package legal_website.services.delete;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import legal_website.dto.DeletePayload;
import legal_website.persistence.auth.UserDeletionStatus;
import legal_website.persistence.auth.UserEntity;
import legal_website.persistence.deletion.UserDeletionProcessEntity;
import legal_website.persistence.outbox_events.OutboxEventEntity;
import legal_website.persistence.auth.UserRepo;
import legal_website.services.outbox.OutboxEventStatusService;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeleteProducerTest {

    @Test
    void publish_sendsOrderDeletionToOrderTopic() {
        TestContext context = context();
        OutboxEventEntity event = event("17", 17L);
        event.setEventType(DeleteOutboxEventType.DELETE_ALL_ORDERS.name());
        stubEventAndUser(context, event, validDeletingUser());
        when(context.kafkaTemplate.send(
                eq("user.delete-orders.requested"),
                eq("17"),
                any(DeletePayload.class)
        )).thenReturn(new CompletableFuture<>());

        context.producer.publishAvailableEvents();

        verify(context.kafkaTemplate).send(
                eq("user.delete-orders.requested"),
                eq("17"),
                any(DeletePayload.class)
        );
    }

    @Test
    void publish_sendsDocumentDeletionToDocumentTopic() {
        TestContext context = context();
        OutboxEventEntity event = event("17", 17L);
        event.setEventType(DeleteOutboxEventType.DELETE_ALL_DOCUMENTS.name());
        stubEventAndUser(context, event, validDeletingUser());
        when(context.kafkaTemplate.send(
                eq("user.delete-documents.requested"),
                eq("17"),
                any(DeletePayload.class)
        )).thenReturn(new CompletableFuture<>());

        context.producer.publishAvailableEvents();

        verify(context.kafkaTemplate).send(
                eq("user.delete-documents.requested"),
                eq("17"),
                any(DeletePayload.class)
        );
    }

    @Test
    void publish_marksDeadWhenAggregateIdIsNotNumber() {
        TestContext context = context();
        OutboxEventEntity event = event("not-a-number", 17L);
        when(context.claimService.claimAvailableEvents(any(), any()))
                .thenReturn(List.of(event));

        context.producer.publishAvailableEvents();

        verify(context.statusService).markDead(
                event,
                "aggregateId must contain numeric userId"
        );
        verify(context.kafkaTemplate, never())
                .send(any(), any(), any(DeletePayload.class));
    }

    @Test
    void publish_marksDeadWhenUserDoesNotExist() {
        TestContext context = context();
        OutboxEventEntity event = event("17", 17L);
        when(context.claimService.claimAvailableEvents(any(), any()))
                .thenReturn(List.of(event));
        when(context.userRepo.findByIdWithDeletionProcess(17L))
                .thenReturn(Optional.empty());

        context.producer.publishAvailableEvents();

        verify(context.statusService).markDead(event, "user not found");
        verify(context.kafkaTemplate, never())
                .send(any(), any(), any(DeletePayload.class));
    }

    @Test
    void publish_marksDeadWhenUserIsStillActive() {
        TestContext context = context();
        OutboxEventEntity event = event("17", 17L);
        UserEntity user = validDeletingUser();
        user.setActive(true);
        stubEventAndUser(context, event, user);

        context.producer.publishAvailableEvents();

        verify(context.statusService)
                .markDead(event, "user is still active");
        verify(context.kafkaTemplate, never())
                .send(any(), any(), any(DeletePayload.class));
    }

    @Test
    void publish_marksDeadWhenDeletionStatusIsNotInProgress() {
        TestContext context = context();
        OutboxEventEntity event = event("17", 17L);
        UserEntity user = validDeletingUser();
        user.setDeletionStatus(UserDeletionStatus.DELETION_FAILED);
        stubEventAndUser(context, event, user);

        context.producer.publishAvailableEvents();

        verify(context.statusService)
                .markDead(event, "user deletion is not in progress");
        verify(context.kafkaTemplate, never())
                .send(any(), any(), any(DeletePayload.class));
    }

    @Test
    void publish_marksDeadWhenDeletionProcessIsMissing() {
        TestContext context = context();
        OutboxEventEntity event = event("17", 17L);
        UserEntity user = validDeletingUser();
        user.setDeletionProcess(null);
        stubEventAndUser(context, event, user);

        context.producer.publishAvailableEvents();

        verify(context.statusService)
                .markDead(event, "user deletion process not found");
        verify(context.kafkaTemplate, never())
                .send(any(), any(), any(DeletePayload.class));
    }

    @Test
    void publish_marksDeadWhenPayloadCannotBeDeserialized() {
        TestContext context = context();
        OutboxEventEntity event = event("17", 17L);
        event.setPayload(JsonNodeFactory.instance.textNode("broken payload"));
        stubEventAndUser(context, event, validDeletingUser());

        context.producer.publishAvailableEvents();

        verify(context.statusService).markDead(
                eq(event),
                contains("invalid delete payload")
        );
        verify(context.kafkaTemplate, never())
                .send(any(), any(), any(DeletePayload.class));
    }

    @Test
    void publish_marksDeadWhenPayloadUserDoesNotMatchAggregate() {
        TestContext context = context();
        OutboxEventEntity event = event("17", 99L);
        stubEventAndUser(context, event, validDeletingUser());

        context.producer.publishAvailableEvents();

        verify(context.statusService).markDead(
                event,
                "payload userId does not match aggregateId"
        );
        verify(context.kafkaTemplate, never())
                .send(any(), any(), any(DeletePayload.class));
    }

    @Test
    void publish_marksFailedWhenEventTypeIsUnsupported() {
        TestContext context = context();
        OutboxEventEntity event = event("17", 17L);
        event.setEventType("UNKNOWN_DELETE_EVENT");
        stubEventAndUser(context, event, validDeletingUser());

        context.producer.publishAvailableEvents();

        verify(context.statusService).markFailed(
                eq(event.getId()),
                eq(event.getProcessingToken()),
                contains("unsupported deletion event type")
        );
        verify(context.kafkaTemplate, never())
                .send(any(), any(), any(DeletePayload.class));
    }

    private void stubEventAndUser(
            TestContext context,
            OutboxEventEntity event,
            UserEntity user
    ) {
        when(context.claimService.claimAvailableEvents(any(), any()))
                .thenReturn(List.of(event));
        when(context.userRepo.findByIdWithDeletionProcess(17L))
                .thenReturn(Optional.of(user));
    }

    private TestContext context() {
        UserRepo userRepo = mock(UserRepo.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, DeletePayload> kafkaTemplate =
                mock(KafkaTemplate.class);
        DeleteOutboxClaimService claimService =
                mock(DeleteOutboxClaimService.class);
        OutboxEventStatusService statusService =
                mock(OutboxEventStatusService.class);
        ThreadPoolTaskExecutor callbackExecutor =
                mock(ThreadPoolTaskExecutor.class);

        DeleteProducer producer = new DeleteProducer(
                userRepo,
                kafkaTemplate,
                new ObjectMapper().findAndRegisterModules(),
                claimService,
                statusService,
                callbackExecutor
        );
        ReflectionTestUtils.setField(
                producer,
                "deleteUserOrdersTopic",
                "user.delete-orders.requested"
        );
        ReflectionTestUtils.setField(
                producer,
                "deleteUserDocumentsTopic",
                "user.delete-documents.requested"
        );
        ReflectionTestUtils.setField(
                producer,
                "processingTimeoutMs",
                180_000L
        );

        return new TestContext(
                producer,
                userRepo,
                kafkaTemplate,
                claimService,
                statusService
        );
    }

    private OutboxEventEntity event(String aggregateId, Long payloadUserId) {
        OutboxEventEntity event = new OutboxEventEntity();
        event.setId(UUID.randomUUID());
        event.setAggregateId(aggregateId);
        event.setAggregateType("USER");
        event.setEventType(DeleteOutboxEventType.DELETE_ALL_ORDERS.name());
        event.setStatus("PROCESSING");
        event.setRetryCount(0);
        event.setProcessingToken(UUID.randomUUID());
        event.setPayload(
                JsonNodeFactory.instance.objectNode()
                        .put("id", payloadUserId)
        );
        return event;
    }

    private UserEntity validDeletingUser() {
        UserEntity user = new UserEntity();
        user.setActive(false);
        user.setDeletionStatus(UserDeletionStatus.DELETION_IN_PROGRESS);

        UserDeletionProcessEntity deletionProcess =
                new UserDeletionProcessEntity();
        deletionProcess.setUser(user);
        deletionProcess.setStatus(UserDeletionStatus.DELETION_IN_PROGRESS);
        user.setDeletionProcess(deletionProcess);
        return user;
    }

    private static class TestContext {
        private final DeleteProducer producer;
        private final UserRepo userRepo;
        private final KafkaTemplate<String, DeletePayload> kafkaTemplate;
        private final DeleteOutboxClaimService claimService;
        private final OutboxEventStatusService statusService;

        private TestContext(
                DeleteProducer producer,
                UserRepo userRepo,
                KafkaTemplate<String, DeletePayload> kafkaTemplate,
                DeleteOutboxClaimService claimService,
                OutboxEventStatusService statusService
        ) {
            this.producer = producer;
            this.userRepo = userRepo;
            this.kafkaTemplate = kafkaTemplate;
            this.claimService = claimService;
            this.statusService = statusService;
        }
    }
}
