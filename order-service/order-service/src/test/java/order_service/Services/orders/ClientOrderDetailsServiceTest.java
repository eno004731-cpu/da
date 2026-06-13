package order_service.Services.orders;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import order_service.Dto.response.ClientOrderDetailsResponse;
import order_service.EntityAndRepo.document.OrderDocumentMetadataEntity;
import order_service.EntityAndRepo.document.OrderDocumentMetadataRepo;
import order_service.EntityAndRepo.order.OrderEntity;
import order_service.EntityAndRepo.order.OrderRepo;
import order_service.Services.documents.DocumentMetadataMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientOrderDetailsServiceTest {

    @Mock
    private OrderRepo orderRepo;

    @Mock
    private OrderDocumentMetadataRepo documentMetadataRepo;

    @Mock
    private DocumentMetadataMapper documentMetadataMapper;

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

        when(orderRepo.findByIdAndClientId(orderId, 7L)).thenReturn(Optional.of(order));
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
        when(orderRepo.findByIdAndClientId(orderId, 7L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> service.getOrderDetails(orderId, 7L));

        assertEquals(404, exception.getStatusCode().value());
    }

    @Test
    void getOrderDetails_throwsNotFoundWhenOrderMissing() {
        UUID orderId = UUID.randomUUID();
        when(orderRepo.findByIdAndClientId(orderId, 7L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> service.getOrderDetails(orderId, 7L));

        assertEquals(404, exception.getStatusCode().value());
    }
}
