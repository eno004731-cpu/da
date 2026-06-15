package order_service.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import order_service.dto.response.ClientOrderSummaryResponse;
import order_service.persistence.order.OrderEntity;
import order_service.persistence.order.OrderRepo;
import order_service.services.orders.ClientOrdersQueryService;

class ClientOrdersQueryServiceIntegrationTest extends PostgresIntegrationTestBase {
    @Autowired
    private ClientOrdersQueryService clientOrdersQueryService;

    @Autowired
    private OrderRepo orderRepo;

    @Test
    void getClientOrders_returnsOnlyCurrentClientOrdersOrderedByCreatedAtDesc() {
        OrderEntity older = orderRepo.save(orderForClient(7L, "OLD", LocalDateTime.parse("2026-06-11T10:00:00")));
        OrderEntity newer = orderRepo.save(orderForClient(7L, "NEW", LocalDateTime.parse("2026-06-12T10:00:00")));
        orderRepo.save(orderForClient(99L, "FOREIGN", LocalDateTime.parse("2026-06-13T10:00:00")));
        orderRepo.save(deletedOrderForClient(7L, "DELETED", LocalDateTime.parse("2026-06-14T10:00:00")));
        orderRepo.save(deletingOrderForClient(7L, "DELETING", LocalDateTime.parse("2026-06-15T10:00:00")));

        List<ClientOrderSummaryResponse> response = clientOrdersQueryService.getClientOrders(7L);

        assertThat(response)
                .extracting(ClientOrderSummaryResponse::getId)
                .containsExactly(newer.getId(), older.getId());
        assertThat(response)
                .extracting(ClientOrderSummaryResponse::getServiceCode)
                .containsExactly("NEW", "OLD");
    }

    private OrderEntity orderForClient(Long clientId, String serviceCode, LocalDateTime createdAt) {
        OrderEntity order = new OrderEntity();
        order.setClientId(clientId);
        order.setClientName("Client " + clientId);
        order.setContact("+79990000000");
        order.setCompanyName("Acme");
        order.setServiceCode(serviceCode);
        order.setTitle("Legal help " + serviceCode);
        order.setProblemDescription("Need legal help");
        order.setStatus("ON_REVIEW");
        order.setCreateAt(createdAt);
        order.setUpdatedAt(createdAt.plusMinutes(5));
        return order;
    }

    private OrderEntity deletedOrderForClient(Long clientId, String serviceCode, LocalDateTime createdAt) {
        OrderEntity order = orderForClient(clientId, serviceCode, createdAt);
        order.setIsDeleted(true);
        order.setDeletedAt(createdAt.plusMinutes(10));
        return order;
    }

    private OrderEntity deletingOrderForClient(Long clientId, String serviceCode, LocalDateTime createdAt) {
        OrderEntity order = orderForClient(clientId, serviceCode, createdAt);
        order.setDeletionInProgress(true);
        return order;
    }
}
