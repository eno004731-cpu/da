package order_service.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import order_service.dto.request.CreateOrderRequest;
import order_service.dto.request.UpdateClientOrderRequest;
import order_service.dto.response.ClientOrderDetailsResponse;
import order_service.dto.response.ClientOrderSummaryResponse;
import order_service.dto.response.CreateOrderResponse;
import order_service.services.orders.ClientOrderDetailsService;
import order_service.services.orders.ClientOrderDeleteService;
import order_service.services.orders.ClientOrdersQueryService;
import order_service.services.orders.ClientOrderUpdateService;
import order_service.services.orders.CreateOrderService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
    private ClientOrdersQueryService clientOrdersQueryService;

    @Mock
    private ClientOrderUpdateService clientOrderUpdateService;

    @Mock
    private ClientOrderDeleteService clientOrderDeleteService;

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
    void getClientOrders_returnsCurrentClientOrders() throws Exception {
        UUID orderId = UUID.randomUUID();
        ClientOrderSummaryResponse response = new ClientOrderSummaryResponse();
        response.setId(orderId);
        response.setTitle("Need legal advice");
        response.setServiceCode("CONSULT");
        response.setServiceName("Юридическая консультация");
        response.setStatus("ON_REVIEW");
        response.setCreatedAt(LocalDateTime.now());
        response.setUpdatedAt(LocalDateTime.now());
        response.setRevisionCount(0);

        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(15L, null, List.of())
        );
        when(clientOrdersQueryService.getClientOrders(15L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/client/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(orderId.toString()))
                .andExpect(jsonPath("$[0].title").value("Need legal advice"))
                .andExpect(jsonPath("$[0].serviceCode").value("CONSULT"))
                .andExpect(jsonPath("$[0].revisionCount").value(0));
    }

    @Test
    void updateOrder_returnsUpdatedOrderDetails() throws Exception {
        UUID orderId = UUID.randomUUID();
        UpdateClientOrderRequest request = new UpdateClientOrderRequest();
        request.setServiceCode("CONTRACT");
        request.setClientName("Client");
        request.setContact("+79990000000");
        request.setDescription("Update contract");

        ClientOrderDetailsResponse response = new ClientOrderDetailsResponse();
        response.setId(orderId);
        response.setTitle("Update contract");
        response.setStatus("ON_REVIEW");

        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(15L, null, List.of())
        );
        when(clientOrderUpdateService.updateOrder(eq(orderId), eq(15L), any(UpdateClientOrderRequest.class)))
                .thenReturn(response);

        mockMvc.perform(patch("/api/client/orders/{orderId}", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.title").value("Update contract"));
    }

    @Test
    void deleteOrder_returnsNoContentAndUsesCurrentClientId() throws Exception {
        UUID orderId = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(15L, null, List.of())
        );

        mockMvc.perform(delete("/api/client/orders/{orderId}", orderId))
                .andExpect(status().isNoContent());

        verify(clientOrderDeleteService).deleteOrder(orderId, 15L);
    }
}
