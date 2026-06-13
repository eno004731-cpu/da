package order_service.Api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import order_service.Dto.request.CreateOrderRequest;
import order_service.Dto.response.ClientOrderDetailsResponse;
import order_service.Dto.response.CreateOrderResponse;
import order_service.Dto.response.UploadedDocumentResponse;
import order_service.Services.orders.ClientOrderDetailsService;
import order_service.Services.orders.CreateOrderService;
import order_service.Services.documents.OrderDocumentsService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ClientOrdersControllerTest {

    @Mock
    private CreateOrderService createOrderService;

    @Mock
    private ClientOrderDetailsService clientOrderDetailsService;

    @Mock
    private OrderDocumentsService orderDocumentsService;

    @InjectMocks
    private ClientOrdersController controller;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        SecurityContextHolder.clearContext();
    }

    @Test
    void createApplication_returnsMinimalCreateResponse() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setServiceCode("CONSULT");
        request.setClientName("Nikita");
        request.setContact("+79990000000");
        request.setDescription("Need legal advice");

        UUID orderId = UUID.randomUUID();
        CreateOrderResponse response = new CreateOrderResponse();
        response.setId(orderId);
        response.setOrderId(orderId);
        response.setStatus("ON_REVIEW");

        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(15L, null, List.of())
        );
        when(createOrderService.createOrder(any(CreateOrderRequest.class), eq(15L))).thenReturn(response);

        mockMvc.perform(post("/api/client/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("ON_REVIEW"));
    }

    @Test
    void getOrderDetails_returnsOrderPayload() throws Exception {
        UUID orderId = UUID.randomUUID();
        ClientOrderDetailsResponse response = new ClientOrderDetailsResponse();
        response.setId(orderId);
        response.setTitle("Need legal advice");
        response.setStatus("ON_REVIEW");
        response.setCreatedAt(LocalDateTime.now());
        response.setUpdatedAt(LocalDateTime.now());
        response.setDocuments(List.of());
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(15L, null, List.of())
        );
        when(clientOrderDetailsService.getOrderDetails(orderId, 15L)).thenReturn(response);

        mockMvc.perform(get("/api/client/orders/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.title").value("Need legal advice"));
    }

    @Test
    void uploadDocuments_returnsUploadedMetadata() throws Exception {
        UUID orderId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("documents", "contract.pdf", "application/pdf", "data".getBytes());
        UploadedDocumentResponse document = new UploadedDocumentResponse();
        document.setId("1");
        document.setFileName("contract.pdf");

        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(7L, null, List.of())
        );
        when(orderDocumentsService.uploadDocuments(eq(orderId), eq(7L), any())).thenReturn(List.of(document));

        mockMvc.perform(multipart("/api/client/orders/{orderId}/documents", orderId)
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[0].fileName").value("contract.pdf"));
    }

    @Test
    void uploadDocuments_returnsNotFoundWhenOrderMissing() throws Exception {
        UUID orderId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("documents", "contract.pdf", "application/pdf", "data".getBytes());

        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(7L, null, List.of())
        );
        when(orderDocumentsService.uploadDocuments(eq(orderId), eq(7L), any()))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Заказ не найден"));

        mockMvc.perform(multipart("/api/client/orders/{orderId}/documents", orderId)
                        .file(file))
                .andExpect(status().isNotFound());
    }
}
