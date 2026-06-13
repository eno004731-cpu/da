package catalog_service.catalog;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServiceCatalogService {
    private final ServiceRepository serviceRepository;

    public ServiceCatalogService(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    public List<ServiceResponseDto> findAllActiveServices() {
        return serviceRepository.findByIsActiveTrueOrderBySortOrderAsc().stream()
                .map(service -> new ServiceResponseDto(
                        service.getId(),
                        service.getCode(),
                        service.getName(),
                        service.getShortDescription()
                ))
                .toList();
    }
}
