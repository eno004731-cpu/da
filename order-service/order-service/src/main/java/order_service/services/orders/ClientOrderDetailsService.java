package order_service.services.orders;

import order_service.dto.response.ClientOrderDetailsResponse;
import order_service.dto.response.UploadedDocumentResponse;
import order_service.persistence.document.OrderDocumentMetadataRepo;
import order_service.persistence.order.OrderEntity;
import order_service.persistence.order.OrderRepo;
import order_service.services.documents.DocumentMetadataMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class ClientOrderDetailsService {
    private final OrderRepo orderRepo;
    private final OrderDocumentMetadataRepo documentMetadataRepo;
    private final DocumentMetadataMapper documentMetadataMapper;

    public ClientOrderDetailsService(
            OrderRepo orderRepo,
            OrderDocumentMetadataRepo documentMetadataRepo,
            DocumentMetadataMapper documentMetadataMapper
    ) {
        this.orderRepo = orderRepo;
        this.documentMetadataRepo = documentMetadataRepo;
        this.documentMetadataMapper = documentMetadataMapper;
    }

    public ClientOrderDetailsResponse getOrderDetails(UUID orderId, Long clientId) {
        OrderEntity order = orderRepo.findByIdAndClientId(orderId, clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Заказ не найден"));

        ClientOrderDetailsResponse response = new ClientOrderDetailsResponse();
        response.setId(order.getId());
        response.setTitle(order.getTitle());
        response.setServiceCode(order.getServiceCode());
        response.setServiceName(order.getServiceName());
        response.setClientName(order.getClientName());
        response.setContact(order.getContact());
        response.setCompanyName(order.getCompanyName());
        response.setProblemDescription(order.getProblemDescription());
        response.setStatus(order.getStatus());
        response.setCreatedAt(order.getCreateAt());
        response.setUpdatedAt(order.getUpdatedAt());
        response.setRevisionCount(0);
        response.setDocuments(loadDocuments(orderId));
        return response;
    }

    private List<UploadedDocumentResponse> loadDocuments(UUID orderId) {
        return documentMetadataRepo.findAllByOrderIdOrderByUploadedAtAsc(orderId).stream()
                .map(documentMetadataMapper::toResponse)
                .toList();
    }
}
