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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import order_service.dto.request.CreateOrderRequest;
import order_service.dto.request.UpdateClientOrderRequest;
import order_service.dto.response.ClientOrderDetailsResponse;
import order_service.dto.response.ClientOrderSummaryResponse;
import order_service.dto.response.CreateOrderResponse;
import order_service.dto.response.UploadedDocumentResponse;
import order_service.services.documents.OrderDocumentDeleteService;
import order_service.services.orders.ClientOrderDetailsService;
import order_service.services.orders.ClientOrderDeleteService;
import order_service.services.orders.ClientOrdersQueryService;
import order_service.services.orders.ClientOrderUpdateService;
import order_service.services.orders.CreateOrderService;
import order_service.services.documents.OrderDocumentsService;

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
    private final OrderDocumentsService orderDocumentsService;
    private final OrderDocumentDeleteService orderDocumentDeleteService;

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

    @PostMapping("/orders/{orderId}/documents")
    public List<UploadedDocumentResponse> uploadDocuments(
            @PathVariable UUID orderId,
            @RequestParam("documents") List<MultipartFile> documents,
            @AuthenticationPrincipal Long clientId
    ) {
        return orderDocumentsService.uploadDocuments(orderId, clientId, documents);
    }

    @DeleteMapping("/orders/{orderId}/documents/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDocument(
            @PathVariable UUID orderId,
            @PathVariable String documentId,
            @AuthenticationPrincipal Long clientId
    ) {
        orderDocumentDeleteService.deleteDocument(orderId, clientId, documentId);
    }
}
