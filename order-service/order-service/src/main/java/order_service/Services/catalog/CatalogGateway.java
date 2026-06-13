package order_service.Services.catalog;

import order_service.Dto.response.ServiceCatalogItemResponse;

import java.util.List;

public interface CatalogGateway {
    List<ServiceCatalogItemResponse> getServices();
}
