package document_service.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import document_service.configs.InternalServiceTokenFilter;
import document_service.dto.response.UploadedDocumentResponse;
import document_service.services.documents.OrderDocumentDeleteService;
import document_service.services.documents.OrderDocumentsQueryService;
import document_service.services.documents.OrderDocumentsService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InternalOrderDocumentsControllerTest {

    @Mock
    private OrderDocumentsService orderDocumentsService;

    @Mock
    private OrderDocumentsQueryService orderDocumentsQueryService;

    @Mock
    private OrderDocumentDeleteService orderDocumentDeleteService;

    @InjectMocks
    private InternalOrderDocumentsController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .addFilter(new InternalServiceTokenFilter("test-token"))
                .build();
    }

    @Test
    void getOrderDocuments_returnsDocumentList() throws Exception {
        UUID orderId = UUID.randomUUID();
        UploadedDocumentResponse response = new UploadedDocumentResponse(
                "1",
                "contract.pdf",
                "application/pdf",
                123L,
                LocalDateTime.now(),
                null,
                false,
                null
        );

        when(orderDocumentsQueryService.listDocuments(orderId)).thenReturn(List.of(response));

        mockMvc.perform(get("/internal/orders/{orderId}/documents", orderId)
                        .header("X-Internal-Service-Token", "test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[0].fileName").value("contract.pdf"));
    }

    @Test
    void uploadDocuments_returnsUploadedMetadata() throws Exception {
        UUID orderId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("documents", "contract.pdf", "application/pdf", "data".getBytes());
        UploadedDocumentResponse response = new UploadedDocumentResponse(
                "1",
                "contract.pdf",
                "application/pdf",
                123L,
                LocalDateTime.now(),
                null,
                false,
                null
        );

        when(orderDocumentsService.uploadDocuments(eq(orderId), eq(9L), any())).thenReturn(List.of(response));

        mockMvc.perform(multipart("/internal/orders/{orderId}/documents", orderId)
                        .header("X-Internal-Service-Token", "test-token")
                        .file(file)
                        .param("uploadedByUserId", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fileName").value("contract.pdf"));
    }

    @Test
    void uploadDocuments_rejectsMissingInternalToken() throws Exception {
        UUID orderId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("documents", "contract.pdf", "application/pdf", "data".getBytes());

        mockMvc.perform(multipart("/internal/orders/{orderId}/documents", orderId)
                        .file(file)
                        .param("uploadedByUserId", "9"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteOrderDocument_requiresTokenAndDelegatesToService() throws Exception {
        UUID orderId = UUID.randomUUID();

        mockMvc.perform(delete("/internal/orders/{orderId}/documents/{documentId}", orderId, "10")
                        .header("X-Internal-Service-Token", "test-token"))
                .andExpect(status().isOk());

        verify(orderDocumentDeleteService).deleteDocument(orderId, "10");
    }

    @Test
    void deleteOrderDocuments_requiresTokenAndDelegatesToService() throws Exception {
        UUID orderId = UUID.randomUUID();

        mockMvc.perform(delete("/internal/orders/{orderId}/documents", orderId)
                        .header("X-Internal-Service-Token", "test-token"))
                .andExpect(status().isOk());

        verify(orderDocumentDeleteService).deleteOrderDocuments(orderId);
    }
}
