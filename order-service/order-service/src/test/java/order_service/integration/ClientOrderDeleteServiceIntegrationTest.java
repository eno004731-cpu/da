package order_service.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;

import order_service.persistence.document.OrderDocumentMetadataEntity;
import order_service.persistence.document.OrderDocumentMetadataRepo;
import order_service.persistence.order.OrderEntity;
import order_service.persistence.order.OrderRepo;
import order_service.services.documents.DocumentGateway;
import order_service.services.documents.OrderDocumentDeleteService;
import order_service.services.orders.ClientOrderDeleteService;

class ClientOrderDeleteServiceIntegrationTest extends PostgresIntegrationTestBase {
    @Autowired
    private ClientOrderDeleteService clientOrderDeleteService;

    @Autowired
    private OrderDocumentDeleteService orderDocumentDeleteService;

    @Autowired
    private DocumentGateway documentGateway;

    @Autowired
    private OrderRepo orderRepo;

    @Autowired
    private OrderDocumentMetadataRepo documentMetadataRepo;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void resetGatewayMock() {
        reset(documentGateway);
    }

    @Test
    void deleteDocument_marksLocalMetadataDeletedAndCallsDocumentService() {
        OrderEntity order = orderRepo.save(orderForClient(7L));
        documentMetadataRepo.save(document(order.getId(), "doc-1"));

        orderDocumentDeleteService.deleteDocument(order.getId(), 7L, "doc-1");

        verify(documentGateway).deleteDocument(order.getId(), "doc-1");
        OrderDocumentMetadataEntity metadata = documentMetadataRepo.findByDocumentId("doc-1").orElseThrow();
        assertThat(metadata.getIsDeleted()).isTrue();
        assertThat(metadata.getDeletedAt()).isNotNull();
    }

    @Test
    void deleteOrder_marksDocumentsDeletedAndHidesOrderFromActiveQueries() {
        OrderEntity order = orderRepo.save(orderForClient(7L));
        documentMetadataRepo.save(document(order.getId(), "doc-1"));
        documentMetadataRepo.save(document(order.getId(), "doc-2"));

        clientOrderDeleteService.deleteOrder(order.getId(), 7L);

        verify(documentGateway).deleteOrderDocuments(order.getId());
        OrderEntity deletedOrder = orderRepo.findById(order.getId()).orElseThrow();
        assertThat(deletedOrder.getIsDeleted()).isTrue();
        assertThat(deletedOrder.getDeletionInProgress()).isFalse();
        assertThat(deletedOrder.getDeletedAt()).isNotNull();
        assertThat(orderRepo.findAllByClientIdOrderByCreateAtDesc(7L)).isEmpty();
        assertThat(documentMetadataRepo.findAllByOrderIdOrderByUploadedAtAsc(order.getId()))
                .allSatisfy(document -> {
                    assertThat(document.getIsDeleted()).isTrue();
                    assertThat(document.getDeletedAt()).isNotNull();
                });
    }

    private OrderEntity orderForClient(Long clientId) {
        LocalDateTime now = LocalDateTime.now();
        OrderEntity order = new OrderEntity();
        order.setClientId(clientId);
        order.setClientName("Client " + clientId);
        order.setContact("+79990000000");
        order.setCompanyName("Acme");
        order.setServiceCode("CONSULT");
        order.setTitle("Legal help");
        order.setProblemDescription("Need legal help");
        order.setStatus("ON_REVIEW");
        order.setCreateAt(now);
        order.setUpdatedAt(now);
        return order;
    }

    private OrderDocumentMetadataEntity document(UUID orderId, String documentId) {
        LocalDateTime now = LocalDateTime.now();
        OrderDocumentMetadataEntity document = new OrderDocumentMetadataEntity();
        document.setDocumentId(documentId);
        document.setOrderId(orderId);
        document.setUploadedByUserId(7L);
        document.setFileName(documentId + ".pdf");
        document.setMimeType("application/pdf");
        document.setSizeBytes(100L);
        document.setUploadedAt(now);
        document.setIsDeleted(false);
        document.setMetadata(objectMapper.createObjectNode().put("documentId", documentId));
        document.setCreatedAt(now);
        document.setUpdatedAt(now);
        return document;
    }
}
