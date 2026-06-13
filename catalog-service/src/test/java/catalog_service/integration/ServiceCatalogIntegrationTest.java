package catalog_service.integration;

import catalog_service.catalog.ServiceCatalogService;
import catalog_service.catalog.ServiceResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceCatalogIntegrationTest extends PostgresCatalogIntegrationTestBase {

    @Autowired
    ServiceCatalogService serviceCatalogService;

    @Test
    void findAllActiveServicesReadsSeededServicesFromPostgres() {
        List<ServiceResponseDto> services = serviceCatalogService.findAllActiveServices();

        assertThat(services)
                .hasSizeGreaterThanOrEqualTo(13)
                .extracting(ServiceResponseDto::code)
                .startsWith("REGISTRATION", "CORPORATE_CHANGES", "DIRECTOR_CHANGE");

        assertThat(services.get(0).name()).isEqualTo("Регистрация ООО / ИП");
    }
}
