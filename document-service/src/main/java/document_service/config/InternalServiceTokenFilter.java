package document_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class InternalServiceTokenFilter extends OncePerRequestFilter {
    private final String internalServiceToken;

    public InternalServiceTokenFilter(@Value("${app.internal.service-token}") String internalServiceToken) {
        this.internalServiceToken = internalServiceToken;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/internal/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String providedToken = request.getHeader("X-Internal-Service-Token");
        if (!internalServiceToken.equals(providedToken)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid internal service token");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
