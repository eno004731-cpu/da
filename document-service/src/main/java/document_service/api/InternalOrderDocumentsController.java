package document_service.api;

import document_service.dto.response.UploadedDocumentResponse;
import document_service.services.documents.OrderDocumentDeleteService;
import document_service.services.documents.OrderDocumentsQueryService;
import document_service.services.documents.OrderDocumentsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/orders/{orderId}/documents")
@RequiredArgsConstructor
public class InternalOrderDocumentsController {
    private final OrderDocumentsService orderDocumentsService;
    private final OrderDocumentsQueryService orderDocumentsQueryService;
    private final OrderDocumentDeleteService orderDocumentDeleteService;

    @GetMapping
    public List<UploadedDocumentResponse> getOrderDocuments(@PathVariable UUID orderId) {
        return orderDocumentsQueryService.listDocuments(orderId);
    }

    @PostMapping
    public List<UploadedDocumentResponse> uploadDocuments(
            @PathVariable UUID orderId,
            @RequestParam("uploadedByUserId") Long uploadedByUserId,
            @RequestParam("documents") List<MultipartFile> documents
    ) {
        return orderDocumentsService.uploadDocuments(orderId, uploadedByUserId, documents);
    }

    @DeleteMapping("/{documentId}")
    public void deleteOrderDocument(
            @PathVariable UUID orderId,
            @PathVariable String documentId
    ) {
        orderDocumentDeleteService.deleteDocument(orderId, documentId);
    }

    @DeleteMapping
    public void deleteOrderDocuments(@PathVariable UUID orderId) {
        orderDocumentDeleteService.deleteOrderDocuments(orderId);
    }
}
