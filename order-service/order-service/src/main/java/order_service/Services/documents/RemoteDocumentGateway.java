package order_service.Services.documents;

import order_service.Dto.response.UploadedDocumentResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class RemoteDocumentGateway implements DocumentGateway {
    private static final ParameterizedTypeReference<List<UploadedDocumentResponse>> DOCUMENT_LIST_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient documentServiceRestClient;
    private final String internalServiceToken;

    public RemoteDocumentGateway(
            @Qualifier("documentServiceRestClient") RestClient documentServiceRestClient,
            @Value("${app.internal.service-token}") String internalServiceToken
    ) {
        this.documentServiceRestClient = documentServiceRestClient;
        this.internalServiceToken = internalServiceToken;
    }

    @Override
    public List<UploadedDocumentResponse> uploadDocuments(UUID orderId, Long uploadedByUserId, List<MultipartFile> documents) {
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("uploadedByUserId", uploadedByUserId);

        for (MultipartFile document : documents) {
            bodyBuilder.part("documents", asResource(document))
                    .filename(document.getOriginalFilename())
                    .contentType(resolveMediaType(document));
        }

        MultiValueMap<String, org.springframework.http.HttpEntity<?>> body = bodyBuilder.build();
        List<UploadedDocumentResponse> response = documentServiceRestClient.post()
                .uri("/internal/orders/{orderId}/documents", orderId)
                .header("X-Internal-Service-Token", internalServiceToken)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(DOCUMENT_LIST_TYPE);
        return response == null ? List.of() : response;
    }

    private ByteArrayResource asResource(MultipartFile document) {
        try {
            byte[] bytes = document.getBytes();
            return new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return document.getOriginalFilename();
                }
            };
        } catch (IOException e) {
            throw new RestClientException("Не удалось прочитать документ для отправки в document-service", e);
        }
    }

    private MediaType resolveMediaType(MultipartFile document) {
        String contentType = document.getContentType();
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        return MediaType.parseMediaType(contentType);
    }
}
