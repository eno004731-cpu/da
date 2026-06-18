package document_service.api;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import document_service.dto.request.SaveRequest;
import document_service.dto.response.SaveResponse;
import document_service.services.documents.SaveDocumentsService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/client/orders")
public class DocumentsController {
    private final SaveDocumentsService saveDocumentsService;
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


}
