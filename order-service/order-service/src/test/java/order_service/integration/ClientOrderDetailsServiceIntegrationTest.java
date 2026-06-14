package order_service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import order_service.dto.response.ClientOrderDetailsResponse;
import order_service.persistence.document.OrderDocumentMetadataEntity;
import order_service.persistence.document.OrderDocumentMetadataRepo;
import order_service.persistence.order.OrderEntity;
import order_service.persistence.order.OrderRepo;
import order_service.services.orders.ClientOrderDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientOrderDetailsServiceIntegrationTest extends PostgresIntegrationTestBase {
    @Autowired
    private ClientOrderDetailsService clientOrderDetailsService;

    @Autowired
    private OrderRepo orderRepo;

    @Autowired
    private OrderDocumentMetadataRepo documentMetadataRepo;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getOrderDetails_readsOnlyClientOrderAndDocumentsFromPostgres() {
        OrderEntity order = orderRepo.save(orderForClient(7L, "CONSULT"));
        orderRepo.save(orderForClient(99L, "OTHER"));
        documentMetadataRepo.save(document(order.getId(), "doc-2", "second.pdf", LocalDateTime.parse("2026-06-11T12:10:00")));
        documentMetadataRepo.save(document(order.getId(), "doc-1", "first.pdf", LocalDateTime.parse("2026-06-11T12:00:00")));

        ClientOrderDetailsResponse response = clientOrderDetailsService.getOrderDetails(order.getId(), 7L);

        assertThat(response.getId()).isEqualTo(order.getId());
        assertThat(response.getServiceCode()).isEqualTo("CONSULT");
        assertThat(response.getDocuments())
                .extracting("id")
                .containsExactly("doc-1", "doc-2");
        assertThat(response.getDocuments().get(0).getDownloadUrl()).isNull();
    }

    @Test
    void getOrderDetails_rejectsForeignClientOrder() {
        OrderEntity order = orderRepo.save(orderForClient(7L, "CONSULT"));

        assertThatThrownBy(() -> clientOrderDetailsService.getOrderDetails(order.getId(), 99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
    }

    private OrderEntity orderForClient(Long clientId, String serviceCode) {
        LocalDateTime now = LocalDateTime.now();
        OrderEntity order = new OrderEntity();
        order.setClientId(clientId);
        order.setClientName("Client " + clientId);
        order.setContact("+79990000000");
        order.setCompanyName("Acme");
        order.setServiceCode(serviceCode);
        order.setTitle("Legal help");
        order.setProblemDescription("Need legal help");
        order.setStatus("ON_REVIEW");
        order.setCreateAt(now);
        order.setUpdatedAt(now);
        return order;
    }

    private OrderDocumentMetadataEntity document(UUID orderId, String documentId, String fileName, LocalDateTime uploadedAt) {
        LocalDateTime now = LocalDateTime.now();
        OrderDocumentMetadataEntity document = new OrderDocumentMetadataEntity();
        document.setDocumentId(documentId);
        document.setOrderId(orderId);
        document.setUploadedByUserId(7L);
        document.setFileName(fileName);
        document.setMimeType("application/pdf");
        document.setSizeBytes(100L);
        document.setUploadedAt(uploadedAt);
        document.setIsDeleted(false);
        document.setMetadata(objectMapper.createObjectNode().put("documentId", documentId));
        document.setCreatedAt(now);
        document.setUpdatedAt(now);
        return document;
    }
}
