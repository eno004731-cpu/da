package order_service.integration;

import order_service.dto.response.ClientOrderDetailsResponse;
import order_service.persistence.order.OrderEntity;
import order_service.persistence.order.OrderRepo;
import order_service.services.orders.ClientOrderDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientOrderDetailsServiceIntegrationTest extends PostgresIntegrationTestBase {
    @Autowired
    private ClientOrderDetailsService clientOrderDetailsService;

    @Autowired
    private OrderRepo orderRepo;

    @Test
    void getOrderDetails_readsOnlyClientOrderFromPostgres() {
        OrderEntity order = orderRepo.save(orderForClient(7L, "CONSULT"));
        orderRepo.save(orderForClient(99L, "OTHER"));

        ClientOrderDetailsResponse response = clientOrderDetailsService.getOrderDetails(order.getId(), 7L);

        assertThat(response.getId()).isEqualTo(order.getId());
        assertThat(response.getServiceCode()).isEqualTo("CONSULT");
    }

    @Test
    void getOrderDetails_rejectsForeignClientOrder() {
        OrderEntity order = orderRepo.save(orderForClient(7L, "CONSULT"));

        assertThatThrownBy(() -> clientOrderDetailsService.getOrderDetails(order.getId(), 99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
    }

    private OrderEntity orderForClient(Long clientId, String serviceCode) {
        LocalDateTime now = LocalDateTime.now();
        OrderEntity order = new OrderEntity();
        order.setClientId(clientId);
        order.setClientName("Client " + clientId);
        order.setContact("+79990000000");
        order.setCompanyName("Acme");
        order.setServiceCode(serviceCode);
        order.setTitle("Legal help");
        order.setProblemDescription("Need legal help");
        order.setStatus("ON_REVIEW");
        order.setCreateAt(now);
        order.setUpdatedAt(now);
        return order;
    }
}
