package order_service.services.orders;

import order_service.dto.response.ClientOrderDetailsResponse;
import order_service.dto.response.UploadedDocumentResponse;
import order_service.persistence.document.OrderDocumentMetadataRepo;
import order_service.persistence.order.OrderEntity;
import order_service.services.documents.DocumentMetadataMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ClientOrderDetailsService {
    private final ClientOrderAccessService clientOrderAccessService;
    private final OrderDocumentMetadataRepo documentMetadataRepo;
    private final DocumentMetadataMapper documentMetadataMapper;
    private final OrderResponseMapper orderResponseMapper;

    public ClientOrderDetailsService(
            ClientOrderAccessService clientOrderAccessService,
            OrderDocumentMetadataRepo documentMetadataRepo,
            DocumentMetadataMapper documentMetadataMapper,
            OrderResponseMapper orderResponseMapper
    ) {
        this.clientOrderAccessService = clientOrderAccessService;
        this.documentMetadataRepo = documentMetadataRepo;
        this.documentMetadataMapper = documentMetadataMapper;
        this.orderResponseMapper = orderResponseMapper;
    }

    public ClientOrderDetailsResponse getOrderDetails(UUID orderId, Long clientId) {
        OrderEntity order = clientOrderAccessService.getClientOrderOrThrow(orderId, clientId);
        return orderResponseMapper.toDetailsResponse(order, loadDocuments(orderId));
    }

    private List<UploadedDocumentResponse> loadDocuments(UUID orderId) {
        return documentMetadataRepo.findAllByOrderIdOrderByUploadedAtAsc(orderId).stream()
                .map(documentMetadataMapper::toResponse)
                .toList();
    }
}
