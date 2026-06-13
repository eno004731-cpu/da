package order_service.Api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import order_service.Dto.request.CreateOrderRequest;
import order_service.Dto.response.ClientOrderDetailsResponse;
import order_service.Dto.response.CreateOrderResponse;
import order_service.Dto.response.UploadedDocumentResponse;
import order_service.Services.orders.ClientOrderDetailsService;
import order_service.Services.orders.CreateOrderService;
import order_service.Services.documents.OrderDocumentsService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/client")
@RequiredArgsConstructor
public class ClientOrdersController {
    private final CreateOrderService createOrderService;
    private final ClientOrderDetailsService clientOrderDetailsService;
    private final OrderDocumentsService orderDocumentsService;

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

    @PostMapping("/orders/{orderId}/documents")
    public List<UploadedDocumentResponse> uploadDocuments(
            @PathVariable UUID orderId,
            @RequestParam("documents") List<MultipartFile> documents,
            @AuthenticationPrincipal Long clientId
    ) {
        return orderDocumentsService.uploadDocuments(orderId, clientId, documents);
    }
}
