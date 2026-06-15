package document_service.documents;

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

    @GetMapping
    public List<UploadedDocumentResponse> getOrderDocuments(@PathVariable UUID orderId) {
        return orderDocumentsService.listDocuments(orderId);
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
        orderDocumentsService.deleteDocument(orderId, documentId);
    }

    @DeleteMapping
    public void deleteOrderDocuments(@PathVariable UUID orderId) {
        orderDocumentsService.deleteOrderDocuments(orderId);
    }
}
