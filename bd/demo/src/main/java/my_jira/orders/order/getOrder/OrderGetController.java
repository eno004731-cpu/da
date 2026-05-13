package my_jira.orders.order.getOrder;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/client/orders")
public class OrderGetController {
    private final OrderGetService ordersGetService;
    @GetMapping("/{id}")
    public OrderRespons getOrder(@PathVariable Long id,Authentication authentication){
        String email= authentication.getName();

        return ordersGetService.getOrder(id,email);
    }

    @GetMapping("/{orderId}/documents/{documentId}/download")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable Long orderId,
            @PathVariable Long documentId,
            Authentication authentication
    ) {
        String email = authentication.getName();

        return ordersGetService.downloadDocument(orderId, documentId, email);
    }
}
