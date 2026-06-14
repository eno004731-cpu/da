package order_service.services.orders;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import order_service.dto.response.ClientOrderDetailsResponse;
import order_service.persistence.document.OrderDocumentMetadataEntity;
import order_service.persistence.document.OrderDocumentMetadataRepo;
import order_service.persistence.order.OrderEntity;
import order_service.services.documents.DocumentMetadataMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientOrderDetailsServiceTest {

    @Mock
    private ClientOrderAccessService clientOrderAccessService;

    @Mock
    private OrderDocumentMetadataRepo documentMetadataRepo;

    @Mock
    private DocumentMetadataMapper documentMetadataMapper;

    @Spy
    private OrderResponseMapper orderResponseMapper = new OrderResponseMapper();

    @InjectMocks
    private ClientOrderDetailsService service;

    @Test
    void getOrderDetails_returnsMappedOrderAndDocuments() {
        UUID orderId = UUID.randomUUID();
        OrderEntity order = new OrderEntity();
        order.setId(orderId);
        order.setTitle("Consultation");
        order.setServiceCode("CONSULT");
        order.setServiceName("Юридическая консультация");
        order.setClientName("Nikita");
        order.setContact("+79990000000");
        order.setCompanyName("Acme");
        order.setProblemDescription("Need help");
        order.setStatus("ON_REVIEW");
        order.setCreateAt(LocalDateTime.now().minusHours(1));
        order.setUpdatedAt(LocalDateTime.now());
        OrderDocumentMetadataEntity document = new OrderDocumentMetadataEntity();
        document.setDocumentId("1");
        document.setFileName("contract.pdf");
        document.setMimeType("application/pdf");
        document.setSizeBytes(123L);
        document.setUploadedAt(LocalDateTime.now());
        document.setIsDeleted(false);

        when(clientOrderAccessService.getClientOrderOrThrow(orderId, 7L)).thenReturn(order);
        when(documentMetadataRepo.findAllByOrderIdOrderByUploadedAtAsc(orderId)).thenReturn(List.of(document));
        when(documentMetadataMapper.toResponse(document)).thenCallRealMethod();

        ClientOrderDetailsResponse response = service.getOrderDetails(orderId, 7L);

        assertEquals(orderId, response.getId());
        assertEquals("Consultation", response.getTitle());
        assertEquals("CONSULT", response.getServiceCode());
        assertEquals("Юридическая консультация", response.getServiceName());
        assertEquals(1, response.getDocuments().size());
        assertEquals("contract.pdf", response.getDocuments().get(0).getFileName());
    }

    @Test
    void getOrderDetails_throwsNotFoundWhenOrderBelongsToAnotherClient() {
        UUID orderId = UUID.randomUUID();
        when(clientOrderAccessService.getClientOrderOrThrow(orderId, 7L))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Заказ не найден"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> service.getOrderDetails(orderId, 7L));

        assertEquals(404, exception.getStatusCode().value());
    }

    @Test
    void getOrderDetails_throwsNotFoundWhenOrderMissing() {
        UUID orderId = UUID.randomUUID();
        when(clientOrderAccessService.getClientOrderOrThrow(orderId, 7L))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Заказ не найден"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> service.getOrderDetails(orderId, 7L));

        assertEquals(404, exception.getStatusCode().value());
    }
}
