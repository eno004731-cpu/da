package order_service.Services.documents;

import order_service.Dto.response.UploadedDocumentResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface DocumentGateway {
    List<UploadedDocumentResponse> uploadDocuments(UUID orderId, Long uploadedByUserId, List<MultipartFile> documents);
}
