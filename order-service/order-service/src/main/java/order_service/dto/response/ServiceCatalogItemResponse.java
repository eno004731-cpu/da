package order_service.dto.response;

public record ServiceCatalogItemResponse(
        Long id,
        String code,
        String name,
        String shortDescription
) {
}
