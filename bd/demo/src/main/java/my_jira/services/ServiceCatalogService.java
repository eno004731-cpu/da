package my_jira.services;

// Нужен список
import java.util.List;

// Можно использовать ArrayList, если будешь собирать DTO через цикл
import java.util.ArrayList;

// Это Spring-аннотация для service layer
import org.springframework.stereotype.Service;

@Service
public class ServiceCatalogService {
    private final ServiceRepository serviceRepository;


    public ServiceCatalogService(ServiceRepository serviceRepository) {
    this.serviceRepository = serviceRepository;
    }
    public List<ServiceResponseDto> findAllActiveServices(){
        List<ServiceEntity> services = serviceRepository.findByIsActiveTrueOrderBySortOrderAsc();
        List<ServiceResponseDto> response = new ArrayList<>();
        for(ServiceEntity service : services){
            ServiceResponseDto dto = new ServiceResponseDto(
                service.getId(),
                service.getCode(),
                service.getName(),
                service.getShortDescription()

            );
            response.add(dto);
        }
        return response;
    }
}
