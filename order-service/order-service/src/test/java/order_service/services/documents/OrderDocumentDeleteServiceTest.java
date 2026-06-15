package order_service.services.documents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import order_service.persistence.document.OrderDocumentMetadataEntity;
import order_service.persistence.document.OrderDocumentMetadataRepo;
import order_service.persistence.order.OrderEntity;
import order_service.services.orders.ClientOrderAccessService;

@ExtendWith(MockitoExtension.class)
class OrderDocumentDeleteServiceTest {
    @Mock
    private ClientOrderAccessService clientOrderAccessService;

    @Mock
    private OrderDocumentMetadataRepo documentMetadataRepo;

    @Mock
    private DocumentGateway documentGateway;

    @InjectMocks
    private OrderDocumentDeleteService service;

    @Test
    void deleteDocument_checksOwnershipCallsDocumentServiceAndMarksMetadataDeleted() {
        UUID orderId = UUID.randomUUID();
        OrderDocumentMetadataEntity metadata = metadata(orderId, "doc-1");

        when(clientOrderAccessService.getClientOrderOrThrow(orderId, 7L)).thenReturn(new OrderEntity());
        when(documentMetadataRepo.findByOrderIdAndDocumentId(orderId, "doc-1")).thenReturn(Optional.of(metadata));

        service.deleteDocument(orderId, 7L, "doc-1");

        verify(documentGateway).deleteDocument(orderId, "doc-1");
        ArgumentCaptor<OrderDocumentMetadataEntity> metadataCaptor = ArgumentCaptor.forClass(OrderDocumentMetadataEntity.class);
        verify(documentMetadataRepo).save(metadataCaptor.capture());
        assertEquals(true, metadataCaptor.getValue().getIsDeleted());
        assertNotNull(metadataCaptor.getValue().getDeletedAt());
    }

    @Test
    void deleteDocument_throwsNotFoundWhenMetadataMissing() {
        UUID orderId = UUID.randomUUID();
        when(clientOrderAccessService.getClientOrderOrThrow(orderId, 7L)).thenReturn(new OrderEntity());
        when(documentMetadataRepo.findByOrderIdAndDocumentId(orderId, "missing")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.deleteDocument(orderId, 7L, "missing")
        );

        assertEquals(404, exception.getStatusCode().value());
    }

    private OrderDocumentMetadataEntity metadata(UUID orderId, String documentId) {
        OrderDocumentMetadataEntity metadata = new OrderDocumentMetadataEntity();
        metadata.setOrderId(orderId);
        metadata.setDocumentId(documentId);
        metadata.setIsDeleted(false);
        metadata.setUpdatedAt(LocalDateTime.now());
        return metadata;
    }
}
