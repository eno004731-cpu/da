package document_service.configs;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RestClientConfig {
    private RestClient restClient;
    @Value("${app.external.order-service-base-url}")
    private String url;
    @Value("${app.internal.service-token}")
    private String internalServiceToken;

    @Bean
    public RestClient checkOrder(RestClient.Builder builder){
        // Один клиент с base URL order-service переиспользуется для всех проверок.
        restClient = builder.baseUrl(url)
                                    .build();
        return restClient;
    }

    public boolean checkOrder(UUID orderId, Long userId) {
        // JWT уже проверен фильтром document-service, поэтому второй сервис получает
        // только извлечённый идентификатор пользователя через доверенный внутренний вызов.
        if (orderId == null || userId == null) {
            throw new IllegalArgumentException("Order ID and user ID must be provided");
        }

        try {
            ResponseEntity<Void> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/internal/check/order")
                            .queryParam("orderId", orderId)
                            .build())
                    // Internal token подтверждает, что запрос пришёл от document-service.
                    .header("X-Internal-Service-Token", internalServiceToken)
                    // userId безопасно передавать только вместе с проверенным internal token.
                    .header("X-User-Id", userId.toString())
                    .retrieve()
                    .toBodilessEntity();

            // Endpoint возвращает пустой ответ: успех определяется статусом 2xx.
            return response.getStatusCode().is2xxSuccessful();
        } catch (HttpClientErrorException.NotFound exception) {
            // 404 означает, что заказ отсутствует, удалён или недоступен пользователю.
            return false;
        }
    }
}
