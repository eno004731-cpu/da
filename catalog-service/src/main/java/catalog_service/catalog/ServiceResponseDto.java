package catalog_service.catalog;

public record ServiceResponseDto(
        Long id,
        String code,
        String name,
        String shortDescription
) {
}
