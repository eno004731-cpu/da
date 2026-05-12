package my_jira.orders.order.getOrders;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;


import lombok.RequiredArgsConstructor;
import my_jira.common.exception.OrderNotFoundException;
import my_jira.common.exception.UserNotFoundException;
import my_jira.orders.order.OrdersEntity;
import my_jira.orders.order.OrdersRepo;
import my_jira.services.ServiceCatalogService;
import my_jira.services.ServiceEntity;
import my_jira.services.ServiceRepository;
import my_jira.users.UsersEntity;
import my_jira.users.UsersRepo;

@Service
@RequiredArgsConstructor
public class GetOrdersService {
    private final UsersRepo usersRepo;
    private final OrdersRepo ordersRepo;
    private final ServiceRepository serviceRepository;
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<ClientOrders> getOrders(String email){

        UsersEntity user = usersRepo.findByEmail(email)
            .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));
       
        List<Long> ordersIds = ordersRepo.findAllIdsByClientUser(user);
        List<ClientOrders> responses = new ArrayList<>();
        for(Long id:ordersIds){
            OrdersEntity order = ordersRepo.findById(id)
                .orElseThrow(()-> new OrderNotFoundException("не найден заказ")); 
            
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