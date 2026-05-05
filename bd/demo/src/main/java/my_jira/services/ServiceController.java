package my_jira.services;
// Нужен список, потому что будем возвращать несколько услуг
import java.util.List;

// Это аннотация REST-контроллера
import org.springframework.web.bind.annotation.RestController;

// Базовый путь для всех методов этого контроллера
import org.springframework.web.bind.annotation.RequestMapping;

// Аннотация для GET-запроса
import org.springframework.web.bind.annotation.GetMapping;


@RestController
@RequestMapping("/api/services")
public class ServiceController {
    private final ServiceCatalogService serviceCatalogService;

    ServiceController(ServiceCatalogService serviceCatalogService){
        this.serviceCatalogService = serviceCatalogService;
    }
    @GetMapping
    public List<ServiceResponseDto> getAllServices(){
        return serviceCatalogService.findAllActiveServices();
        
    }
    


}
