package order_service.services.orders;

import java.util.List;

import org.springframework.stereotype.Component;

import order_service.dto.response.ClientOrderDetailsResponse;
import order_service.dto.response.ClientOrderSummaryResponse;
import order_service.dto.response.UploadedDocumentResponse;
import order_service.persistence.order.OrderEntity;

@Component
public class OrderResponseMapper {
    public ClientOrderDetailsResponse toDetailsResponse(
            OrderEntity order,
            List<UploadedDocumentResponse> documents
    ) {
        ClientOrderDetailsResponse response = new ClientOrderDetailsResponse();
        mapCommonFields(order, response);
        response.setClientName(order.getClientName());
        response.setContact(order.getContact());
        response.setCompanyName(order.getCompanyName());
        response.setProblemDescription(order.getProblemDescription());
        response.setDocuments(documents);
        return response;
    }

    public ClientOrderSummaryResponse toSummaryResponse(OrderEntity order) {
        ClientOrderSummaryResponse response = new ClientOrderSummaryResponse();
        mapCommonFields(order, response);
        return response;
    }

    private void mapCommonFields(OrderEntity order, ClientOrderDetailsResponse response) {
        response.setId(order.getId());
        response.setTitle(order.getTitle());
        response.setServiceCode(order.getServiceCode());
        response.setServiceName(order.getServiceName());
        response.setStatus(order.getStatus());
        response.setCreatedAt(order.getCreateAt());
        response.setUpdatedAt(order.getUpdatedAt());
        response.setRevisionCount(0);
    }

    private void mapCommonFields(OrderEntity order, ClientOrderSummaryResponse response) {
        response.setId(order.getId());
        response.setTitle(order.getTitle());
        response.setServiceCode(order.getServiceCode());
        response.setServiceName(order.getServiceName());
        response.setStatus(order.getStatus());
        response.setCreatedAt(order.getCreateAt());
        response.setUpdatedAt(order.getUpdatedAt());
        response.setRevisionCount(0);
    }
}
