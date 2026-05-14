package my_jira.orders.order.delOrder;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/client/orders")
@RequiredArgsConstructor
public class OrderDelController {
    private final OrderDelService orderDelService;
    @DeleteMapping("/{orderId}")
    public boolean deleteOrder(Authentication authentication,@PathVariable("orderId") Long id){
        String email=authentication.getName();

       
        return orderDelService.deleteOrder(email,id);
    }
}
