package order_service.services.catalog;

import order_service.dto.response.ServiceCatalogItemResponse;

import java.util.List;

public interface CatalogGateway {
    List<ServiceCatalogItemResponse> getServices();
}
