package order_service.services.documents;

import order_service.dto.response.UploadedDocumentResponse;
import order_service.services.orders.ClientOrderAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class OrderDocumentsService {
    private final ClientOrderAccessService clientOrderAccessService;
    private final DocumentGateway documentGateway;

    public OrderDocumentsService(ClientOrderAccessService clientOrderAccessService, DocumentGateway documentGateway) {
        this.clientOrderAccessService = clientOrderAccessService;
        this.documentGateway = documentGateway;
    }

    public List<UploadedDocumentResponse> uploadDocuments(UUID orderId, Long clientId, List<MultipartFile> documents) {
        if (documents == null || documents.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Нужно передать хотя бы один документ");
        }
        if (documents.stream().anyMatch(MultipartFile::isEmpty)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Пустые документы загружать нельзя");
        }
        clientOrderAccessService.getClientOrderOrThrow(orderId, clientId);
        return documentGateway.uploadDocuments(orderId, clientId, documents);
    }
}
