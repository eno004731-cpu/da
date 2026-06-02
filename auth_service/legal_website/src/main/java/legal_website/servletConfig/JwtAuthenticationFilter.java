package legal_website.servletConfig;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import legal_website.EntityAndRepo.Auth.UserEntity;
import legal_website.Services.Jwt.JwtService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    @Qualifier("jwtAuthenticationEntryPoint")
    private final AuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        // Если Bearer token нет, ничего не валидируем.
        // Дальше Spring Security сам решит: это public endpoint или нужен 401.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authHeader.substring(7);

            UserEntity user = jwtService.getUserFromValidAccessToken(token);
            Authentication authentication = UsernamePasswordAuthenticationToken
                .authenticated(
                    user.getId(),
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
                );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (RuntimeException ex) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(
                request,
                response,
                new BadCredentialsException("Невалидный access token", ex)
            );
            return;
        }

        // Ошибки из controller/service не должны маскироваться под 401 токена.
        filterChain.doFilter(request, response);

    }
    
}
