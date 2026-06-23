package order_service.services.events.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import order_service.dto.payload.DocumentStoredPayload;
import order_service.persistence.events.outbox.OutboxEventEntity;
import order_service.persistence.events.outbox.OutboxEventRepo;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DocumentValidationOutboxServiceTest {

    @Test
    void createSuccessfulValidationEvent_savesRealValidationContract() throws Exception {
        OutboxEventRepo repository = mock(OutboxEventRepo.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        DocumentValidationOutboxService service =
                new DocumentValidationOutboxService(repository, objectMapper);
        UUID orderId = UUID.randomUUID();

        DocumentStoredPayload storedDocument = new DocumentStoredPayload();
        storedDocument.setDocumentId("101");
        storedDocument.setOrderId(orderId);

        service.createSuccessfulValidationEvent(storedDocument);

        ArgumentCaptor<OutboxEventEntity> captor =
                ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(repository).save(captor.capture());
        OutboxEventEntity event = captor.getValue();

        assertThat(event.getId()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(orderId);
        assertThat(event.getEventType()).isEqualTo("DOCUMENT_VALIDATION_RESULT");
        assertThat(event.getStatus()).isEqualTo("NEW");
        assertThat(event.getRetryCount()).isZero();
        assertThat(event.getPayload().get("eventId").asText())
                .isEqualTo(event.getId().toString());
        assertThat(event.getPayload().get("documentId").asLong()).isEqualTo(101L);
        assertThat(event.getPayload().get("orderId").asText()).isEqualTo(orderId.toString());
        assertThat(event.getPayload().get("validationPassed").asBoolean()).isTrue();
        LocalDateTime validatedAt = objectMapper.treeToValue(
                event.getPayload().get("validatedAt"),
                LocalDateTime.class
        );
        assertThat(validatedAt).isNotNull();
        assertThat(validatedAt).isBeforeOrEqualTo(LocalDateTime.now());
    }
}
