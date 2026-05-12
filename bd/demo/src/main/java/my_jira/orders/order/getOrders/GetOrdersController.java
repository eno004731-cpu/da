package my_jira.orders.order.getOrders;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/client/orders")
@RequiredArgsConstructor
public class GetOrdersController {
    private final GetOrdersService getOrdersService;
    @GetMapping("/orders")
    public List< ClientOrders> getOrders(Authentication authentication){

        String email = authentication.getName();


        return getOrdersService.getOrders(email);
    }
}
