package order_service.api;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import order_service.services.CheckOrder;

@RestController
@RequestMapping("api/internal")
@RequiredArgsConstructor
public class DocumentsController {
    private final CheckOrder checkOrder;

    @GetMapping("/check/order")
    public void checkOrder(
            @RequestParam UUID orderId,
            // JWT уже проверен в document-service, а доверие к заголовку обеспечивает internal token.
            @RequestHeader("X-User-Id") Long userId
    ){
        checkOrder.checkOrder(orderId, userId);
    }
}
