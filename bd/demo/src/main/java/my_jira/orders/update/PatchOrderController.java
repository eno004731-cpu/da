package my_jira.orders.update;


import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import my_jira.orders.details.OrderResponse;

@RestController
@RequestMapping("/api/client/orders")
@RequiredArgsConstructor
class PatchOrderController {
    private final PatchService patchService;
    @PatchMapping("/{orderId}")
    public OrderResponse patchOrder(@RequestBody PatchDto requst
        , Authentication authentication,
        @PathVariable("orderId")Long id){
        
        String email = authentication.getName();
        return patchService.patchOrder(email, requst,id);
    }
}