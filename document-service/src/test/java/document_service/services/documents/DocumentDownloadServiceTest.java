package document_service.services.documents;

import document_service.persistence.document.DocumentEntity;
import document_service.persistence.document.DocumentRepository;
import document_service.persistence.document.DocumentValidationStatus;
import document_service.services.documents.store.DocumentFileStorage;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DocumentDownloadServiceTest {

    @Test
    void download_returnsResourceForValidatedDocumentOwnedByUser() {
        DocumentRepository repository = mock(DocumentRepository.class);
        DocumentFileStorage storage = mock(DocumentFileStorage.class);
        DocumentDownloadService service = new DocumentDownloadService(repository, storage);
        Resource resource = mock(Resource.class);
        UUID orderId = UUID.randomUUID();
        DocumentEntity entity = document(10L, orderId, 7L);
        entity.setValidationStatus(DocumentValidationStatus.DOCUMENT_VALIDATED);

        when(repository.findByIdAndOrderIdAndUploadedByUserId(10L, orderId, 7L))
                .thenReturn(Optional.of(entity));
        when(storage.loadAsResource(entity.getStorageKey())).thenReturn(resource);

        DocumentDownloadService.DownloadedDocument result =
                service.download(orderId, 7L, 10L);

        assertThat(result.getResource()).isSameAs(resource);
        assertThat(result.getFileName()).isEqualTo("contract.pdf");
        assertThat(result.getMimeType()).isEqualTo("application/pdf");
        assertThat(result.getSize()).isEqualTo(123L);
        verify(storage).loadAsResource(entity.getStorageKey());
    }

    @Test
    void download_rejectsDocumentThatHasNotPassedValidation() {
        DocumentRepository repository = mock(DocumentRepository.class);
        DocumentFileStorage storage = mock(DocumentFileStorage.class);
        DocumentDownloadService service = new DocumentDownloadService(repository, storage);
        UUID orderId = UUID.randomUUID();
        DocumentEntity entity = document(10L, orderId, 7L);
        entity.setValidationStatus(DocumentValidationStatus.DOCUMENT_VALIDATION_REQUESTED);

        when(repository.findByIdAndOrderIdAndUploadedByUserId(10L, orderId, 7L))
                .thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.download(orderId, 7L, 10L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("ещё не прошёл проверку");

        // До физического storage запрос не должен доходить.
        verifyNoInteractions(storage);
    }

    private DocumentEntity document(Long id, UUID orderId, Long userId) {
        DocumentEntity entity = new DocumentEntity();
        entity.setId(id);
        entity.setOrderId(orderId);
        entity.setUploadedByUserId(userId);
        entity.setOriginalFileName("contract.pdf");
        entity.setStorageKey(orderId + "/contract.pdf");
        entity.setMimeType("application/pdf");
        entity.setSizeBytes(123L);
        entity.setIsDeleted(false);
        entity.setDocumentDeleted(false);
        return entity;
    }
}
