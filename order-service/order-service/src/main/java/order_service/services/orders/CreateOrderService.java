package order_service.services.orders;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import order_service.dto.request.CreateOrderRequest;
import order_service.dto.response.CreateOrderResponse;
import order_service.persistence.order.OrderEntity;
import order_service.persistence.order.OrderRepo;
import order_service.services.catalog.ServiceNameOutboxService;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreateOrderService {
    private final OrderRepo orderRepo;
    private final ServiceNameOutboxService serviceNameOutboxService;

    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest request,Long id){
        OrderEntity order = createOrderEntity(request, id);
        orderRepo.save(order);
        serviceNameOutboxService.createServiceNameRequestedEvent(order);
        // Персональные поля заявки не пишем в лог — достаточно идентификаторов.
        log.info("Order created orderId={} clientId={} serviceCode={}",
                order.getId(), id, order.getServiceCode());

        CreateOrderResponse response = new CreateOrderResponse();
        response.setId(order.getId());
        response.setOrderId(order.getId());
        response.setStatus(order.getStatus());
        return response;
    }

    private OrderEntity createOrderEntity(CreateOrderRequest request,Long id){
        String description = request.getDescription().trim();
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setClientId(id);
        orderEntity.setClientName(request.getClientName());
        orderEntity.setCompanyName(request.getCompanyName());
        orderEntity.setContact(request.getContact());
        orderEntity.setServiceCode(request.getServiceCode());

        // В title держим короткий человекочитаемый заголовок, а полное описание отдельно.
        orderEntity.setTitle(buildTitle(description));
        orderEntity.setProblemDescription(description);
        orderEntity.setStatus("ON_REVIEW");
        orderEntity.setCreateAt(LocalDateTime.now());
        orderEntity.setUpdatedAt(LocalDateTime.now());
        orderEntity.setIsDeleted(false);
        orderEntity.setDeletionInProgress(false);
        return orderEntity;
    }

    private String buildTitle(String description) {
        if (description.length() <= 255) {
            return description;
        }
        return description.substring(0, 252) + "...";
    }
}
