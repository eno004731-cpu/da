package document_service.api;

import java.util.List;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import document_service.dto.request.SaveRequest;
import document_service.dto.response.SaveResponse;
import document_service.dto.response.UploadedDocumentResponse;
import document_service.services.documents.DocumentDownloadService;
import document_service.services.documents.OrderDocumentsQueryService;
import document_service.services.documents.delete.DeleteDocument;
import document_service.services.documents.store.SaveDocumentsService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/client/orders")
public class DocumentsController {
    private final SaveDocumentsService saveDocumentsService;
    private final DeleteDocument deleteDocument;
    private final OrderDocumentsQueryService orderDocumentsQueryService;
    private final DocumentDownloadService documentDownloadService;

    @GetMapping("/{orderId}/documents")
    public List<UploadedDocumentResponse> getDocuments(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal Long clientId
    ) {
        // Пользователь получает только документы, которые загрузил сам.
        return orderDocumentsQueryService.listDocuments(orderId, clientId);
    }

    @GetMapping("/{orderId}/documents/{documentId}/download")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable UUID orderId,
            @PathVariable Long documentId,
            @AuthenticationPrincipal Long clientId
    ) {
        DocumentDownloadService.DownloadedDocument document =
                documentDownloadService.download(orderId, clientId, documentId);

        // Content-Disposition сообщает браузеру исходное имя сохраняемого файла.
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(document.getFileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.getMimeType()))
                .contentLength(document.getSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(document.getResource());
    }

    @PostMapping("/{orderId}/documents")
    public List<SaveResponse> saveDocuments(
        @PathVariable("orderId") UUID orderId,
        @RequestPart("documents") List<MultipartFile> documents,
        // JwtAuthenticationFilter уже проверил токен и положил userId в principal.
        @AuthenticationPrincipal Long userId
        
    ){
        SaveRequest saveRequest = new SaveRequest();
        saveRequest.setDocuments(documents);
        saveRequest.setUserId(userId);
        saveRequest.setOrderId(orderId);
        return saveDocumentsService.saveDocument(saveRequest);
    }
    @DeleteMapping("/{orderId}/documents/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDocument(
            @PathVariable UUID orderId,
            @PathVariable Long documentId,
            @AuthenticationPrincipal Long clientId
    ) {
        deleteDocument.deleteDocument(orderId, clientId, documentId);
    }
}
