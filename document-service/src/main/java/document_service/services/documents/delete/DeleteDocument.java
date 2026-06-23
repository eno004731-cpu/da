package document_service.services.documents.delete;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import document_service.persistence.document.DocumentEntity;
import document_service.persistence.document.DocumentRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeleteDocument {
    private final DocumentRepository documentRepository;


    @Transactional
    public void deleteDocument(UUID orderId, Long userId, Long documentId){
        DocumentEntity entity = documentRepository.findByIdAndOrderIdAndUploadedByUserId(documentId, orderId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "не найден документ"));
        entity.setDocumentDeleted(true);
        documentRepository.save(entity);
    }
}
