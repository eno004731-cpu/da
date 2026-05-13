package my_jira.orders.order.getOrders;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;


import lombok.RequiredArgsConstructor;
import my_jira.common.exception.UserNotFoundException;
import my_jira.orders.order.OrdersEntity;
import my_jira.orders.order.OrdersRepo;
import my_jira.services.ServiceEntity;
import my_jira.users.UsersEntity;
import my_jira.users.UsersRepo;

@Service
@RequiredArgsConstructor
public class GetOrdersService {
    private final UsersRepo usersRepo;
    private final OrdersRepo ordersRepo;
    
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<ClientOrders> getOrders(String email){

        UsersEntity user = usersRepo.findByEmail(email)
            .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));
       
        
        List<OrdersEntity> orders= ordersRepo.findAllByClientUserWithService(user);
        List<ClientOrders> responses = new ArrayList<>();
        for(OrdersEntity order:orders){

            ClientOrders response = new ClientOrders();
            response.setId(order.getId());
            response.setTitle(order.getTitle());
            
            ServiceEntity service = order.getService();
            response.setServiceCode(service.getCode());
            response.setServiceName(service.getName());
            response.setStatus(order.getStatus());
            response.setCreatedAt(order.getCreatedAt());
            response.setUpdatedAt(order.getUpdatedAt());
            response.setRevisionCount(order.getRevisionCount());
            responses.add(response);
        }
        return responses; 

        
    }
    
}