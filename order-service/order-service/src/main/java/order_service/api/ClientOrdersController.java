package order_service.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/client")
@RequiredArgsConstructor
public class ClientOrdersController {
    private final CreateOrderService createOrderService;
    private final ClientOrderDetailsService clientOrderDetailsService;
    private final ClientOrdersQueryService clientOrdersQueryService;
    private final ClientOrderUpdateService clientOrderUpdateService;
    private final ClientOrderDeleteService clientOrderDeleteService;

    @PostMapping("/applications")
    public CreateOrderResponse createApplication(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal Long clientId
    ) {
        return createOrderService.createOrder(request, clientId);
    }

    @GetMapping("/orders/{orderId}")
    public ClientOrderDetailsResponse getOrderDetails(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal Long clientId
    ) {
        return clientOrderDetailsService.getOrderDetails(orderId, clientId);
    }

    @GetMapping("/orders")
    public List<ClientOrderSummaryResponse> getClientOrders(
            @AuthenticationPrincipal Long clientId
    ) {
        return clientOrdersQueryService.getClientOrders(clientId);
    }

    @PatchMapping("/orders/{orderId}")
    public ClientOrderDetailsResponse updateOrder(
            @PathVariable UUID orderId,
            @Valid @RequestBody UpdateClientOrderRequest request,
            @AuthenticationPrincipal Long clientId
    ) {
        return clientOrderUpdateService.updateOrder(orderId, clientId, request);
    }

    @DeleteMapping("/orders/{orderId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOrder(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal Long clientId
    ) {
        clientOrderDeleteService.deleteOrder(orderId, clientId);
    }

}
