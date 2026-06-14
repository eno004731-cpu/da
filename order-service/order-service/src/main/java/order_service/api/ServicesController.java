package order_service.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import order_service.dto.response.ServiceCatalogItemResponse;
import order_service.services.catalog.CatalogGateway;

import java.util.List;

@RestController
@RequestMapping("/api/services")
public class ServicesController {
    private final CatalogGateway catalogGateway;

    public ServicesController(CatalogGateway catalogGateway) {
        this.catalogGateway = catalogGateway;
    }

    @GetMapping
    public List<ServiceCatalogItemResponse> getServices() {
        return catalogGateway.getServices();
    }
}
