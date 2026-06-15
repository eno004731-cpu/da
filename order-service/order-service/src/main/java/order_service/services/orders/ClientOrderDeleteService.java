package order_service.services.orders;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import order_service.persistence.document.OrderDocumentMetadataEntity;
import order_service.persistence.document.OrderDocumentMetadataRepo;
import order_service.persistence.order.OrderEntity;
import order_service.persistence.order.OrderRepo;
import order_service.services.documents.DocumentGateway;

@Service
@RequiredArgsConstructor
public class ClientOrderDeleteService {
    private final ClientOrderAccessService clientOrderAccessService;
    private final OrderDocumentMetadataRepo documentMetadataRepo;
    private final DocumentGateway documentGateway;
    private final OrderRepo orderRepo;

    public void deleteOrder(UUID orderId, Long clientId) {
        OrderEntity order = clientOrderAccessService.getClientOrderOrThrow(orderId, clientId);
        markDeletionInProgress(order);

        try {
            documentGateway.deleteOrderDocuments(orderId);
            LocalDateTime deletedAt = LocalDateTime.now();
            documentMetadataRepo.findAllByOrderIdOrderByUploadedAtAsc(orderId)
                    .forEach(document -> markDocumentDeleted(document, deletedAt));
            finalizeOrderDelete(order, deletedAt);
        } catch (Exception e) {
            markDeletionFailed(order, e.toString());
            throw e;
        }
    }

    private void markDeletionInProgress(OrderEntity order) {
        order.setDeletionInProgress(true);
        order.setDeletionError(null);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepo.save(order);
    }

    private void markDocumentDeleted(OrderDocumentMetadataEntity document, LocalDateTime deletedAt) {
        document.setIsDeleted(true);
        document.setDeletedAt(deletedAt);
        document.setUpdatedAt(LocalDateTime.now());
        documentMetadataRepo.save(document);
    }

    private void finalizeOrderDelete(OrderEntity order, LocalDateTime deletedAt) {
        order.setIsDeleted(true);
        order.setDeletedAt(deletedAt);
        order.setDeletionInProgress(false);
        order.setDeletionError(null);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepo.save(order);
    }

    private void markDeletionFailed(OrderEntity order, String error) {
        order.setDeletionInProgress(false);
        order.setDeletionError(error);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepo.save(order);
    }
}
