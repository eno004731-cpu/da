package document_service.services.documents;

import document_service.dto.response.UploadedDocumentResponse;
import document_service.persistence.document.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Отвечает только за чтение документов заказа.
 */
@Service
@RequiredArgsConstructor
public class OrderDocumentsQueryService {
    private final DocumentRepository documentRepository;
    private final DocumentResponseMapper responseMapper;

    @Transactional(readOnly = true)
    public List<UploadedDocumentResponse> listDocuments(UUID orderId, Long uploadedByUserId) {
        // Entity не выходит за пределы service-слоя: наружу возвращается DTO.
        return documentRepository
                .findAllByOrderIdAndUploadedByUserIdOrderByCreatedAtAsc(orderId, uploadedByUserId)
                .stream()
                .map(responseMapper::toResponse)
                .toList();
    }
}
