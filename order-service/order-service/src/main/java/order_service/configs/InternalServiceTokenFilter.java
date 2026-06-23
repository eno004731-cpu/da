package order_service.configs;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class InternalServiceTokenFilter extends OncePerRequestFilter {
    // Spring подставляет общий секрет для внутренних межсервисных запросов.
    @Value("${app.internal.service-token}")
    private String internalServiceToken;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        // Обычные пользовательские endpoint продолжают проверяться JWT-фильтром.
        if (!request.getRequestURI().startsWith("/api/internal/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String providedToken = request.getHeader("X-Internal-Service-Token");
        if (!internalServiceToken.equals(providedToken)) {
            // Сам token никогда не записываем в лог.
            log.warn("Internal request rejected method={} uri={} remoteAddress={}",
                    request.getMethod(), request.getRequestURI(), request.getRemoteAddr());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid internal service token");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
