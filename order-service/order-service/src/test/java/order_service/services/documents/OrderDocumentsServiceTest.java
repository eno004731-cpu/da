package order_service.services.documents;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import order_service.dto.response.UploadedDocumentResponse;
import order_service.persistence.order.OrderEntity;
import order_service.services.orders.ClientOrderAccessService;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderDocumentsServiceTest {

    @Mock
    private ClientOrderAccessService clientOrderAccessService;

    @Mock
    private DocumentGateway documentGateway;

    @InjectMocks
    private OrderDocumentsService service;

    @Test
    void uploadDocuments_throwsBadRequestWhenNoFilesProvided() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.uploadDocuments(UUID.randomUUID(), 1L, List.of())
        );

        assertEquals(400, exception.getStatusCode().value());
    }

    @Test
    void uploadDocuments_throwsNotFoundWhenOrderMissing() {
        UUID orderId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("documents", "contract.pdf", "application/pdf", "data".getBytes());
        when(clientOrderAccessService.getClientOrderOrThrow(orderId, 1L))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Заказ не найден"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.uploadDocuments(orderId, 1L, List.of(file))
        );

        assertEquals(404, exception.getStatusCode().value());
    }

    @Test
    void uploadDocuments_delegatesToDocumentGateway() {
        UUID orderId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("documents", "contract.pdf", "application/pdf", "data".getBytes());
        UploadedDocumentResponse response = new UploadedDocumentResponse();
        response.setId("1");
        when(clientOrderAccessService.getClientOrderOrThrow(orderId, 7L)).thenReturn(new OrderEntity());
        when(documentGateway.uploadDocuments(orderId, 7L, List.of(file))).thenReturn(List.of(response));

        List<UploadedDocumentResponse> uploaded = service.uploadDocuments(orderId, 7L, List.of(file));

        assertEquals(1, uploaded.size());
        verify(documentGateway).uploadDocuments(orderId, 7L, List.of(file));
    }
}
