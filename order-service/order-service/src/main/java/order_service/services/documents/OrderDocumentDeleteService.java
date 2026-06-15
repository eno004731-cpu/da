package order_service.services.documents;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import order_service.persistence.document.OrderDocumentMetadataEntity;
import order_service.persistence.document.OrderDocumentMetadataRepo;
import order_service.services.orders.ClientOrderAccessService;

@Service
@RequiredArgsConstructor
public class OrderDocumentDeleteService {
    private final ClientOrderAccessService clientOrderAccessService;
    private final OrderDocumentMetadataRepo documentMetadataRepo;
    private final DocumentGateway documentGateway;

    @Transactional
    public void deleteDocument(UUID orderId, Long clientId, String documentId) {
        clientOrderAccessService.getClientOrderOrThrow(orderId, clientId);
        OrderDocumentMetadataEntity metadata = documentMetadataRepo.findByOrderIdAndDocumentId(orderId, documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Документ не найден"));

        documentGateway.deleteDocument(orderId, documentId);
        markDeleted(metadata, LocalDateTime.now());
    }

    public void markDeleted(OrderDocumentMetadataEntity metadata, LocalDateTime deletedAt) {
        metadata.setIsDeleted(true);
        metadata.setDeletedAt(deletedAt);
        metadata.setUpdatedAt(LocalDateTime.now());
        documentMetadataRepo.save(metadata);
    }
}
