package order_service.Dto.response;

public record ServiceCatalogItemResponse(
        Long id,
        String code,
        String name,
        String shortDescription
) {
}
