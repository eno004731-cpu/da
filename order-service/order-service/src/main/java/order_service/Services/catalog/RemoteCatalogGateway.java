package order_service.Services.catalog;

import order_service.Dto.response.ServiceCatalogItemResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class RemoteCatalogGateway implements CatalogGateway {
    private static final ParameterizedTypeReference<List<ServiceCatalogItemResponse>> SERVICES_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient catalogServiceRestClient;

    public RemoteCatalogGateway(@Qualifier("catalogServiceRestClient") RestClient catalogServiceRestClient) {
        this.catalogServiceRestClient = catalogServiceRestClient;
    }

    @Override
    public List<ServiceCatalogItemResponse> getServices() {
        List<ServiceCatalogItemResponse> response = catalogServiceRestClient.get()
                .uri("/api/services")
                .retrieve()
                .body(SERVICES_TYPE);
        return response == null ? List.of() : response;
    }
}
