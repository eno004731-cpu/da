package order_service.services.orders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import order_service.persistence.document.OrderDocumentMetadataEntity;
import order_service.persistence.document.OrderDocumentMetadataRepo;
import order_service.persistence.order.OrderEntity;
import order_service.persistence.order.OrderRepo;
import order_service.services.documents.DocumentGateway;

@ExtendWith(MockitoExtension.class)
class ClientOrderDeleteServiceTest {
    @Mock
    private ClientOrderAccessService clientOrderAccessService;

    @Mock
    private OrderDocumentMetadataRepo documentMetadataRepo;

    @Mock
    private DocumentGateway documentGateway;

    @Mock
    private OrderRepo orderRepo;

    @InjectMocks
    private ClientOrderDeleteService service;

    @Test
    void deleteOrder_marksInProgressDeletesRemoteDocumentsAndFinalizesSoftDelete() {
        UUID orderId = UUID.randomUUID();
        OrderEntity order = new OrderEntity();
        order.setIsDeleted(false);
        OrderDocumentMetadataEntity document = new OrderDocumentMetadataEntity();

        when(clientOrderAccessService.getClientOrderOrThrow(orderId, 7L)).thenReturn(order);
        when(documentMetadataRepo.findAllByOrderIdOrderByUploadedAtAsc(orderId)).thenReturn(List.of(document));

        service.deleteOrder(orderId, 7L);

        verify(documentGateway).deleteOrderDocuments(orderId);
        assertEquals(true, document.getIsDeleted());
        assertNotNull(document.getDeletedAt());
        assertEquals(true, order.getIsDeleted());
        assertEquals(false, order.getDeletionInProgress());
        assertNotNull(order.getDeletedAt());
        verify(documentMetadataRepo).save(document);
    }

    @Test
    void deleteOrder_recordsFailureWhenDocumentServiceFails() {
        UUID orderId = UUID.randomUUID();
        OrderEntity order = new OrderEntity();
        order.setIsDeleted(false);
        RuntimeException failure = new RuntimeException("document-service unavailable");

        when(clientOrderAccessService.getClientOrderOrThrow(orderId, 7L)).thenReturn(order);
        doThrow(failure).when(documentGateway).deleteOrderDocuments(orderId);

        RuntimeException result = assertThrows(RuntimeException.class, () -> service.deleteOrder(orderId, 7L));

        assertEquals(failure, result);
        assertEquals(false, order.getDeletionInProgress());
        assertEquals(false, order.getIsDeleted());
        assertEquals("java.lang.RuntimeException: document-service unavailable", order.getDeletionError());

        ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepo, org.mockito.Mockito.atLeast(2)).save(orderCaptor.capture());
    }
}
