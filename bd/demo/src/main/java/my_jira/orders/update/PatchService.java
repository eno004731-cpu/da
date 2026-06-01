package my_jira.orders.update;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;


import lombok.RequiredArgsConstructor;
import my_jira.common.exception.OrderNotFoundException;
import my_jira.common.exception.ServiceNotFoundException;
import my_jira.common.exception.UserNotFoundException;
import my_jira.orders.OrdersEntity;
import my_jira.orders.OrdersRepo;
import my_jira.orders.details.OrderGetService;
import my_jira.orders.details.OrderResponse;
import my_jira.catalog.ServiceEntity;
import my_jira.catalog.ServiceRepository;
import my_jira.auth.UsersEntity;
import my_jira.auth.UsersRepo;

@Service
@RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional(readOnly = false)
public class PatchService {
    private final OrdersRepo ordersRepo;
    private final UsersRepo usersRepo;
    private final ServiceRepository serviceRepository;
    private final OrderGetService orderGetService;
    public OrderResponse patchOrder(String email,PatchDto patchDto,Long orderId){
        
        OrdersEntity order = getOwnedOrder(orderId, email);
        ServiceEntity service = serviceRepository.findByCode(patchDto.getServiceCode());
        if(service == null){
            throw new ServiceNotFoundException("не удалось найти услугу");
        }
        order.setService(service);
        order.setTitle(service.getName());
        order.setClientNameSnapshot(patchDto.getClientName());
        order.setContactSnapshot(patchDto.getContact());
        order.setCompanyNameSnapshot(patchDto.getCompanyName());
        order.setDescription(patchDto.getDescription());
        order.setUpdatedAt(LocalDateTime.now());
        ordersRepo.save(order);


        return orderGetService.getOrder(orderId, email);
    }


        private OrdersEntity getOwnedOrder(Long orderId, String email) {
        // Ищем текущего пользователя по email из сессии.
        UsersEntity user = usersRepo.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

        // Проверяем, что заказ принадлежит текущему пользователю.
        // Если нет, возвращаем "не найден", чтобы не раскрывать чужие id.
        List<Long> idsOrders = ordersRepo.findAllIdsByClientUser(user);
        if (!idsOrders.contains(orderId)) {
            throw new OrderNotFoundException("Заказ не найден");
        }

        return ordersRepo.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Заказ не найден"));
    }
}
